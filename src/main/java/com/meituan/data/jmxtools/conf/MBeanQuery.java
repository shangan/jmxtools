package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.meituan.data.jmxtools.utils.Preconditions.checkNotNull;

public class MBeanQuery {
    private final String name;
    private final String objectName;
    private final List<List<String>> attributes;

    @JsonCreator
    public MBeanQuery(@JsonProperty("name") String name,
                      @JsonProperty("objectName") String objectName,
                      @JsonProperty("attributes") List<List<String>> attributes) {
        this.name = checkNotNull(name, "name is null");
        this.objectName = checkNotNull(objectName, "objectName is null");
        this.attributes = checkNotNull(attributes, "attributes is null");
    }

    public String getName() {
        return name;
    }

    public String getObjectName() {
        return objectName;
    }

    public List<List<String>> getAttributes() {
        return attributes;
    }
}
