package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meituan.data.jmxtools.jmx.Metric;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CustomMetricGroup extends MetricGroup {
    private final String resolverClassName;
    private final Map<String, String> config;

    @JsonCreator
    public CustomMetricGroup(@JsonProperty(value = "group") String groupName,
                             @JsonProperty(value = "class") String resolverClassName,
                             @JsonProperty(value = "config") Map<String, String> config) {
        super(groupName);
        this.resolverClassName = checkNotNull(resolverClassName, "class is null");
        if (config == null) {
            this.config = Collections.emptyMap();
        } else {
            this.config = config;
        }
    }

    @JsonProperty("class")
    public String getResolverClassName() {
        return resolverClassName;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    @Override
    public Collection<Metric> resolveMetrics(MBeanServerConnection connection) throws IOException {
        // TODO
        return null;
    }
}
