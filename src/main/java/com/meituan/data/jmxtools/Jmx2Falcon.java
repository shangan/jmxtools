package com.meituan.data.jmxtools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.meituan.data.jmxtools.conf.Conf;
import com.meituan.data.jmxtools.conf.Endpoint;
import com.meituan.data.jmxtools.jmx.JmxConnections;
import com.meituan.data.jmxtools.jmx.JmxQueryException;
import com.meituan.data.jmxtools.jmx.JmxQueryExecutor;
import com.meituan.data.jmxtools.reporter.Metric;
import com.meituan.data.jmxtools.reporter.MetricsReportException;
import com.meituan.data.jmxtools.reporter.Reporter;
import com.meituan.data.jmxtools.reporter.Reporters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
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

import static com.google.common.base.Preconditions.checkNotNull;

public class Jmx2Falcon {
    static final Logger LOG = LoggerFactory.getLogger(Jmx2Falcon.class);

    static class EndpointProcessor implements Runnable {
        private static int JMX_CONNECT_TIMEOUT_SECOND = 20;
        private Conf conf;
        private Endpoint endpoint;
        private Reporter reporter;

        public EndpointProcessor(Conf conf, Endpoint endpoint) {
            this.conf = checkNotNull(conf, "conf is null");
            this.endpoint = checkNotNull(endpoint, "endpoint is null");
            this.reporter = Reporters.newReporter(conf.getReporterConf());
        }

        @Override
        public void run() {
            try {
                boolean jmxAlive = false;
                List<Metric> metrics = Lists.newArrayList();

                // connect to JMX endpoint and collect metrics
                try (JMXConnector connection = JmxConnections.connectWithTimeout(endpoint, JMX_CONNECT_TIMEOUT_SECOND, TimeUnit.SECONDS)) {
                    jmxAlive = true;
                    LOG.info("Connected to {}", endpoint.getName());

                    JmxQueryExecutor executor = new JmxQueryExecutor(connection);
                    metrics = executor.executeAll(conf.getQueries());

                } catch (IOException e) {
                    LOG.info("Cannot connect to {}", endpoint.getName());
                    LOG.debug("The exception stack:", e);

                } catch (JmxQueryException e) {
                    LOG.error("Failed to query metrics", e);

                }

                metrics.add(createAliveMetric(jmxAlive));

                // report metrics
                try {
                    String serviceHost = endpoint.isRemote() ? endpoint.getRemoteHost() : getLocalShortHostName();
                    reporter.report(serviceHost, conf.getServiceName(), metrics);
                    LOG.info("Successfully report {} metrics to {}", metrics.size(), endpoint.getName());

                } catch (MetricsReportException e) {
                    LOG.error("Failed to report metrics", e);
                } catch (UnknownHostException e) {
                    LOG.error("Failed to get host name", e);
                }

            } catch (Throwable throwable) {
                LOG.error("Unexpected exception", throwable);
            }
        }

        private Metric createAliveMetric(boolean isAlive) {
            final String metricName = conf.getServiceName() + "." + "jmx.alive";
            return new Metric(metricName, isAlive ? 1 : 0, Metric.Type.GAUGE);
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
            System.err.println("Usage: Jmx2Falcon <path-to-config-file>");
            System.exit(1);
        }

        Conf conf = loadConf(new FileInputStream(new File(args[0])));
        ExecutorService pool = Executors.newFixedThreadPool(conf.getEndpoints().size());

        // Each endpoint is collected and reported in its own thread
        for (Endpoint endpoint : conf.getEndpoints()) {
            pool.submit(new EndpointProcessor(conf, endpoint));
        }

        pool.shutdown();
        try {
            boolean notTimeout = pool.awaitTermination(40, TimeUnit.SECONDS);
            if (!notTimeout) {
                LOG.info("Timeout! Shut down threads.");
            }

        } catch (InterruptedException e) {
            LOG.warn("Main thread was interrupted");
        }
        pool.shutdownNow();
    }
}
