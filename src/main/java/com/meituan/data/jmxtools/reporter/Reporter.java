package com.meituan.data.jmxtools.reporter;

import com.meituan.data.jmxtools.jmx.Metric;

import java.util.List;

public interface Reporter {

    /**
     * Report all metrics in the list to a monitoring system.
     * NOTE that currently only Numeric metrics are allowed.
     *
     * @param serviceHost host of the service these metrics belongs to
     * @param serviceName name of the service these metrics belongs to
     * @param metrics all the metrics collected for the service
     * @throws MetricsReportException
     */
    void report(String serviceHost, String serviceName, List<Metric> metrics) throws MetricsReportException;
}
