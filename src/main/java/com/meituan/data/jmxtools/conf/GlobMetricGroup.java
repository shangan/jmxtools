package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meituan.data.jmxtools.jmx.GlobMetricResolver;
import com.meituan.data.jmxtools.jmx.Metric;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class GlobMetricGroup extends MetricGroup {
    private final String objectNameString;
    private final List<String> gauges;
    private final List<String> counters;

    private final ObjectName objectName;

    @JsonCreator
    public GlobMetricGroup(@JsonProperty(value = "group") String groupName,
                           @JsonProperty(value = "objectName") String objectNameString,
                           @JsonProperty(value = "gauges") List<String> gauges,
                           @JsonProperty(value = "counters", required = false) List<String> counters) {
        super(groupName);
        this.objectNameString = checkNotNull(objectNameString, "objectName is null");
        this.gauges = checkNotNull(gauges, "gauges is null");
        if (counters == null) {
            this.counters = Collections.emptyList();
        } else {
            this.counters = counters;
        }

        try {
            this.objectName = new ObjectName(objectNameString);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("invalid objectName: " + objectNameString, e);
        }
    }

    @JsonProperty("objectName")
    public String getObjectNameString() {
        return objectNameString;
    }

    public List<String> getGauges() {
        return gauges;
    }

    public List<String> getCounters() {
        return counters;
    }

    @JsonIgnore
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public Collection<Metric> resolveMetrics(MBeanServerConnection connection) throws IOException {
        return new GlobMetricResolver(connection).resolve(this);
    }
}
