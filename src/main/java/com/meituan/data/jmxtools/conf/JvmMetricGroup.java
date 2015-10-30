package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meituan.data.jmxtools.jmx.jvm.JvmMetricResolver;
import com.meituan.data.jmxtools.jmx.Metric;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.util.Collection;

public final class JvmMetricGroup extends MetricGroup {

    @JsonCreator
    public JvmMetricGroup(@JsonProperty(value = "group") String groupName) {
        super(groupName);
    }

    @Override
    public Collection<Metric> resolveMetrics(MBeanServerConnection connection) throws IOException {
        return new JvmMetricResolver(connection).resolve(this);
    }
}
