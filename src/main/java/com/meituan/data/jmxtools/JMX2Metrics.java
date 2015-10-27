package com.meituan.data.jmxtools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.meituan.data.jmxtools.conf.Conf;
import com.meituan.data.jmxtools.conf.Endpoint;
import com.meituan.data.jmxtools.conf.GlobMetricGroup;
import com.meituan.data.jmxtools.conf.MetricGroup;
import com.meituan.data.jmxtools.jmx.JmxConnections;
import com.meituan.data.jmxtools.jmx.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The class collects and dumps all the metrics according to
 * the passed in configuration file or endpoint.
 */
public class Jmx2Metrics implements AutoCloseable {
    static final Logger LOG = LoggerFactory.getLogger(Jmx2Metrics.class);
    private static int JMX_CONNECT_TIMEOUT_SECOND = 20;

    static Conf loadConf(InputStream confInput) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        return mapper.readValue(confInput, Conf.class);
    }

    private final Endpoint endpoint;
    private final List<MetricGroup> metricGroups;
    private final JMXConnector connector;
    private final MBeanServerConnection connection;

    public Jmx2Metrics(Endpoint endpoint) throws IOException {
        this.endpoint = checkNotNull(endpoint, "endpoint is null");
        this.connector = JmxConnections.connectWithTimeout(endpoint, JMX_CONNECT_TIMEOUT_SECOND, TimeUnit.SECONDS);
        this.connection = connector.getMBeanServerConnection();
        // list all metrics under the endpoint
        this.metricGroups = Lists.newArrayList();
        for (ObjectName name : connection.queryNames(null, null)) {
            String nameStr = name.getCanonicalName();
            MetricGroup group = new GlobMetricGroup(nameStr, nameStr, Lists.newArrayList("*"), null);
            metricGroups.add(group);
        }
    }

    public Jmx2Metrics(Conf conf) throws IOException {
        checkNotNull(conf, "conf is null");
        this.endpoint = conf.getEndpoints().get(0);
        this.connector = JmxConnections.connectWithTimeout(endpoint, JMX_CONNECT_TIMEOUT_SECOND, TimeUnit.SECONDS);
        this.connection = connector.getMBeanServerConnection();
        this.metricGroups = conf.getMetricGroups();
    }

    public SortedMap<String, Metric> collect() {
        SortedMap<String, Metric> result = Maps.newTreeMap();

        for (MetricGroup group : metricGroups) {
            try {
                for (Metric metric : group.resolveMetrics(connection)) {
                    result.put(metric.getName(), metric);
                }
            } catch (IOException e) {
                LOG.error("Error processing group " + group.getGroupName(), e);
            }
        }

        return result;
    }

    @Override
    public void close() {
        if (connector != null) {
            LOG.debug("Closing connection to {}", endpoint);
            try {
                connector.close();
            } catch (IOException e) {
                LOG.error("Failed to close " + connector, e);
            }
        }
    }

    static void printUsageAndExit() {
        final String usage = "Usage:\n" +
                "collect <endpoint>\n" +
                "        --conf <path-to-config-file>\n\n" +
                "where <endpoint> is one of:\n" +
                "local:process-regex     for local JVM\n" +
                "remote:host:port        for remote JVM\n";
        System.err.println(usage);
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            printUsageAndExit();
        }

        Collection<Metric> metrics;

        if (args[0].equals("--conf") || args[0].equals("-conf")) {
            if (args.length != 2) {
                printUsageAndExit();
            }

            Conf conf = loadConf(new FileInputStream(args[1]));
            try (Jmx2Metrics driver = new Jmx2Metrics(conf)) {
                metrics = driver.collect().values();
            }

        } else {
            Endpoint endpoint = Endpoint.valueOf(args[0]);

            try (Jmx2Metrics driver = new Jmx2Metrics(endpoint)) {
                metrics = driver.collect().values();
            }
        }

        for (Metric metric : metrics) {
            System.out.println(metric.getName() + "\t" + metric.getValue());
        }
    }
}
