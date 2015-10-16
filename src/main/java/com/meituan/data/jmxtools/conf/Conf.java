package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.meituan.data.jmxtools.utils.Preconditions.checkNotNull;

/**
 * Represent metrics configuration for a service.
 */
public class Conf {
    private final String serviceName;
    private final List<Endpoint> endpoints;
    private final List<MBeanQuery> queries;

    @JsonCreator
    public Conf(@JsonProperty("serviceName") String serviceName,
                @JsonProperty("endpoints") List<Endpoint> endpoints,
                @JsonProperty("queries") List<MBeanQuery> queries) {
        this.serviceName = checkNotNull(serviceName, "serviceName is null");
        this.endpoints = checkNotNull(endpoints, "endpoints is null");
        this.queries = checkNotNull(queries, "queries is null");
    }

    public String getServiceName() {
        return serviceName;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public List<MBeanQuery> getQueries() {
        return queries;
    }
}
