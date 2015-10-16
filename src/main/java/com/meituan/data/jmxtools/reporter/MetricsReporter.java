package com.meituan.data.jmxtools.reporter;

import com.meituan.data.jmxtools.utils.Tuple2;

import java.util.List;

import static com.meituan.data.jmxtools.utils.Preconditions.checkNotNull;

/**
 * Base class for all reporter.
 */
public abstract class MetricsReporter {

    protected String serviceHost;
    protected String serviceName;
    protected long timestamp;

    public MetricsReporter(String serviceHost, String serviceName) {
        this.serviceHost = checkNotNull(serviceHost, "serviceHost is null");
        this.serviceName = checkNotNull(serviceName, "serviceName is null");
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    /**
     * Report all metrics in the list to a monitoring system.
     * NOTE that currently only GAUGE and Numeric metrics are allowed.
     *
     * @param metrics
     * @throws MetricsReportException
     */
    public abstract void report(List<Tuple2<String, Number>> metrics) throws MetricsReportException;
}
