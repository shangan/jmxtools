package com.meituan.data.jmxtools.jmx;

import com.meituan.data.jmxtools.conf.MetricGroup;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class MetricResolver<T extends MetricGroup> {

    protected final MBeanServerConnection connection;

    public MetricResolver(MBeanServerConnection connection) {
        this.connection = checkNotNull(connection, "connection is null");
    }

    public abstract Collection<Metric> resolve(T metricGroup) throws IOException;
}
