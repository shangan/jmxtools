package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a leaf attribute of a MBean.
 */
public class AttributeConf {
    private final String name; // dot-concatenated attribute hierarchy, such as HeapMemoryUsage.used
    private final String type; // metric type, one of [gauge, counter], case-insensitive

    @JsonCreator
    public AttributeConf(@JsonProperty("name") String name,
                         @JsonProperty("type") String type) {
        this.name = checkNotNull(name);
        this.type = checkNotNull(type);

        String lowerCaseType = type.toLowerCase();
        checkArgument("gauge".equals(lowerCaseType) || "counter".equals(lowerCaseType),
                      "type should be one of [gauge, counter], but was: " + type);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
