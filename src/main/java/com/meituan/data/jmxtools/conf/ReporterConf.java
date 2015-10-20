package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents configurations for metrics reporter.
 */
public class ReporterConf {
    private final String type;
    private final Map<String, String> options;

    @JsonCreator
    public ReporterConf(@JsonProperty("type") String type,
                        @JsonProperty("options") Map<String, String> options) {
        this.type = checkNotNull(type, "type is null").toLowerCase();
        this.options = checkNotNull(options, "options is null");

        checkArgument(type.equals("falcon"), "Invalid reporter type: %s, only \"falcon\" is supported", type);
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getOptions() {
        return options;
    }
}
