package com.meituan.data.jmxtools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.meituan.data.jmxtools.conf.Conf;
import com.meituan.data.jmxtools.conf.Endpoint;
import com.meituan.data.jmxtools.conf.MetricGroup;
import com.meituan.data.jmxtools.jmx.JmxConnections;
import com.meituan.data.jmxtools.jmx.Metric;
import com.meituan.data.jmxtools.reporter.MetricsReportException;
import com.meituan.data.jmxtools.reporter.Reporter;
import com.meituan.data.jmxtools.reporter.Reporters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
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
                try (JMXConnector connector = JmxConnections.connectWithTimeout(endpoint, JMX_CONNECT_TIMEOUT_SECOND, TimeUnit.SECONDS)) {
                    MBeanServerConnection connection = connector.getMBeanServerConnection();
                    jmxAlive = true;
                    LOG.info("Connected to {}", endpoint.getName());

                    for (MetricGroup metricGroup : conf.getMetricGroups()) {
                        metrics.addAll(metricGroup.resolveMetrics(connection));
                    }

                } catch (IOException e) {
                    LOG.error("Failed to collect metrics of " + endpoint, e);
                }

                metrics.add(new Metric("jmx.alive", jmxAlive ? 1 : 0, Metric.Type.GAUGE));

                // report metrics
                try {
                    reporter.report(getServiceHost(endpoint), conf.getServiceName(), metrics);
                    LOG.info("Successfully report {} metrics of {}", metrics.size(), endpoint.getName());

                } catch (MetricsReportException e) {
                    LOG.error("Failed to report metrics", e);
                } catch (UnknownHostException e) {
                    LOG.error("Failed to get host name", e);
                }

            } catch (Throwable throwable) {
                LOG.error("Unexpected exception", throwable);
            }
        }

        private String getServiceHost(Endpoint endpoint) throws UnknownHostException {
            if (!endpoint.isRemote() || "localhost".equals(endpoint.getRemoteHost())) {
                String hostname = InetAddress.getLocalHost().getHostName();
                String[] parts = hostname.split("\\.");
                return (parts.length > 1) ? parts[0] : hostname;
            }
            return endpoint.getRemoteHost();
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
