package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;

public class TestReadConf {
    public static void main(String[] args) throws IOException {
        InputStream is = TestReadConf.class.getResourceAsStream("/example/hivemetastore.json");

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Conf hmsConf = mapper.readValue(is, Conf.class);
        System.out.println(mapper.writeValueAsString(hmsConf));
    }
}
