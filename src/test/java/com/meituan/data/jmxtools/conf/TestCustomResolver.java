package com.meituan.data.jmxtools.conf;

import com.google.common.collect.Lists;
import com.meituan.data.jmxtools.jmx.Metric;
import com.meituan.data.jmxtools.jmx.MetricResolver;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class TestCustomResolver extends MetricResolver<CustomMetricGroup> {

    protected TestCustomResolver(MBeanServerConnection connection) {
        super(connection);
    }

    @Override
    public Collection<Metric> resolve(CustomMetricGroup metricGroup) throws IOException {
        // dummy implementation using config key as metric name
        List<Metric> metrics = Lists.newArrayList();
        for (String key : metricGroup.getConfig().keySet()) {
            metrics.add(new Metric(metricGroup.getGroupName() + "." + key, 0, Metric.Type.GAUGE));
        }
        return metrics;
    }
}
