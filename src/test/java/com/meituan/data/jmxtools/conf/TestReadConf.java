package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

// FIXME more elaborate tests based on JUnit
public class TestReadConf {
    public static void main(String[] args) throws IOException {
        InputStream is = new FileInputStream(new File("example/hivemetastore.json"));

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        Conf hmsConf = mapper.readValue(is, Conf.class);
        System.out.println(mapper.writeValueAsString(hmsConf));
    }
}
