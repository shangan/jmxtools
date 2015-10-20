package com.meituan.data.jmxtools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meituan.data.jmxtools.conf.Conf;
import com.meituan.data.jmxtools.conf.Endpoint;
import com.meituan.data.jmxtools.jmx.JmxConnections;
import com.meituan.data.jmxtools.jmx.JmxQueryException;
import com.meituan.data.jmxtools.jmx.JmxQueryExecutor;
import com.meituan.data.jmxtools.reporter.Metric;

import javax.management.remote.JMXConnector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * The class collects and dumps all the metrics value according to
 * the passed in configuration file[s].
 */
public class Jmx2Metrics {

    static Conf loadConf(InputStream confInput) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        return mapper.readValue(confInput, Conf.class);
    }

    static void handleEndpoint(Conf conf, Endpoint endpoint) throws IOException, JmxQueryException {
        try (JMXConnector connection = JmxConnections.connectWithTimeout(endpoint, 20, TimeUnit.SECONDS)) {
            JmxQueryExecutor executor = new JmxQueryExecutor(connection);

            for (Metric metric : executor.executeAll(conf.getQueries())) {
                System.out.println(metric.getName() + "\t" + metric.getValue());
            }
        }
    }

    public static void main(String[] args) throws IOException, JmxQueryException {

        for (String arg : args) {
            System.out.println("collect metrics defined in " + arg);
            System.out.println("---------------------------------");
            Conf conf = loadConf(new FileInputStream(new File(arg)));

            for (Endpoint endpoint : conf.getEndpoints()) {
                System.out.println("\nEndpoint: " + endpoint.getName());
                handleEndpoint(conf, endpoint);
            }
        }
    }
}
