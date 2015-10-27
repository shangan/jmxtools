package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.meituan.data.jmxtools.jmx.Metric;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "resolver",
        defaultImpl = GlobMetricGroup.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GlobMetricGroup.class, name = "glob"),
        @JsonSubTypes.Type(value = CustomMetricGroup.class, name = "custom")
})
public abstract class MetricGroup {
    protected final String groupName;

    public MetricGroup(@JsonProperty(value = "group") String groupName) {
        this.groupName = checkNotNull(groupName, "group is null");
    }

    @JsonProperty("group")
    public String getGroupName() {
        return groupName;
    }

    public abstract Collection<Metric> resolveMetrics(MBeanServerConnection connection) throws IOException;
}
