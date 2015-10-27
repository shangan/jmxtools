package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represent metrics collecting & reporting configuration for a service.
 */
public class Conf {
    private final String serviceName;
    private final ReporterConf reporterConf;
    private final List<Endpoint> endpoints;
    private final List<MetricGroup> metricGroups;

    @JsonCreator
    public Conf(@JsonProperty("serviceName") String serviceName,
                @JsonProperty("reporter") ReporterConf reporterConf,
                @JsonProperty("endpoints") List<Endpoint> endpoints,
                @JsonProperty("metrics") List<MetricGroup> metricGroups) {
        this.serviceName = checkNotNull(serviceName, "serviceName is null");
        this.reporterConf = checkNotNull(reporterConf, "reporter is null");
        this.endpoints = checkNotNull(endpoints, "endpoints is null");
        this.metricGroups = checkNotNull(metricGroups, "metrics is null");

        checkArgument(endpoints.size() > 0, "endpoints is empty");
    }

    public String getServiceName() {
        return serviceName;
    }

    @JsonProperty("reporter")
    public ReporterConf getReporterConf() {
        return reporterConf;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    @JsonProperty("metrics")
    public List<MetricGroup> getMetricGroups() {
        return metricGroups;
    }
}
