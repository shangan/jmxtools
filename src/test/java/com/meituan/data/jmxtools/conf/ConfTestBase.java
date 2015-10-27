package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;

public class ConfTestBase {
    static <T> T fromFile(String fileName, Class<T> valueType) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        InputStream is = ConfTestBase.class.getResourceAsStream("/" + fileName);
        return mapper.readValue(is, valueType);
    }
}
