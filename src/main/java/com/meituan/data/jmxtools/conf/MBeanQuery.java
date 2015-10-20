package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MBeanQuery {
    private final String groupName;
    private final String objectName;
    private final List<AttributeConf> attributes;

    @JsonCreator
    public MBeanQuery(@JsonProperty("groupName") String groupName,
                      @JsonProperty("objectName") String objectName,
                      @JsonProperty("attributes") List<AttributeConf> attributes) {
        this.groupName = checkNotNull(groupName, "groupName is null");
        this.objectName = checkNotNull(objectName, "objectName is null");
        this.attributes = checkNotNull(attributes, "attributes is null");
    }

    public String getGroupName() {
        return groupName;
    }

    public String getObjectName() {
        return objectName;
    }

    public List<AttributeConf> getAttributes() {
        return attributes;
    }
}
