package com.meituan.data.jmxtools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meituan.data.jmxtools.conf.Conf;
import com.meituan.data.jmxtools.conf.Endpoint;
import com.meituan.data.jmxtools.jmx.JmxQueryExecutor;
import com.meituan.data.jmxtools.reporter.FalconReporter;
import com.meituan.data.jmxtools.utils.DaemonThreadFactory;
import com.meituan.data.jmxtools.utils.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.meituan.data.jmxtools.utils.Preconditions.checkNotNull;

public class Jmx2Falcon {
    static final Logger LOG = LoggerFactory.getLogger(Jmx2Falcon.class);

    static class EndpointRunner implements Runnable {
        private final Conf conf;
        private final Endpoint endpoint;

        public EndpointRunner(Conf conf, Endpoint endpoint) {
            this.conf = checkNotNull(conf, "conf is null");
            this.endpoint = checkNotNull(endpoint, "endpoint is null");
        }

        @Override
        public void run() {
            try (JmxQueryExecutor executor = new JmxQueryExecutor(endpoint)) {
                // fetch jxm metrics
                List<Tuple2<String, Number>> metrics = executor.executeAll(conf.getQueries());

                // report to falcon
                String serviceHost = endpoint.isRemote() ? endpoint.getRemoteHost() : getLocalShortHostName();
                FalconReporter reporter = new FalconReporter(serviceHost, conf.getServiceName());
                reporter.report(metrics);

            } catch (Exception e) {
                LOG.error("Failed to report metrics on {}", endpoint, e);
            }
        }

        private String getLocalShortHostName() throws UnknownHostException {
            String hostname = InetAddress.getLocalHost().getHostName();
            String[] parts = hostname.split("\\.");
            return (parts.length > 1) ? parts[0] : hostname;
        }
    }

    static Conf loadConf(InputStream confInput) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        return mapper.readValue(confInput, Conf.class);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            LOG.error("Usage: Jmx2Falcon <path-to-config-file>");
            System.exit(1);
        }

        Conf conf = loadConf(new FileInputStream(new File(args[0])));
        ExecutorService pool = Executors.newFixedThreadPool(conf.getEndpoints().size(), DaemonThreadFactory.instance);

        // Each endpoint is collected and reported in its own thread
        for (Endpoint endpoint : conf.getEndpoints()) {
            pool.submit(new EndpointRunner(conf, endpoint));
        }

        pool.shutdown();
        try {
            boolean notTimeout = pool.awaitTermination(40, TimeUnit.SECONDS);
            if (notTimeout) {
                LOG.info("Jmx2Falcon runs successfully with {} endpoints", conf.getEndpoints().size());
            } else {
                LOG.warn("Timeout! Shut down threads.");
                pool.shutdownNow();
            }

        } catch (InterruptedException e) {
            LOG.warn("Main thread was interrupted");
            pool.shutdownNow();
        }
    }
}
