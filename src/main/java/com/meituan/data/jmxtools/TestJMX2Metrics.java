package com.meituan.data.jmxtools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meituan.data.jmxtools.conf.Conf;
import com.meituan.data.jmxtools.conf.Endpoint;
import com.meituan.data.jmxtools.jmx.JmxQueryException;
import com.meituan.data.jmxtools.jmx.JmxQueryExecutor;
import com.meituan.data.jmxtools.utils.Tuple2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

// TODO refactor into a runnable tools
public class TestJMX2Metrics {

    static Conf loadConf(InputStream confInput) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        return mapper.readValue(confInput, Conf.class);
    }

    public static void main(String[] args) throws IOException, JmxQueryException {

        Conf conf = loadConf(new FileInputStream(new File(args[0])));

        for (Endpoint endpoint : conf.getEndpoints()) {
            try (JmxQueryExecutor executor = new JmxQueryExecutor(endpoint)) {
                for (Tuple2<String, Number> item : executor.executeAll(conf.getQueries())) {
                    System.out.println(item);
                }
            }
        }
    }
}
