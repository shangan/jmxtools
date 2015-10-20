package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Ints;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a JMX endpoint.
 *
 * <ul>
 *     <li>For remote endpoint, `name` is of format "hostname:port"</li>
 *     <li>For local endpoint, `name` is a regex matching JVM process name</li>
 * </ul>
 *
 */
public class Endpoint {
    private final String name;
    private final boolean isRemote;

    // only valid when isRemote == true
    private String remoteHost;
    private int remotePort;

    @JsonCreator
    public Endpoint(@JsonProperty("name") String name,
                    @JsonProperty("remote") boolean isRemote) {
        this.name = checkNotNull(name, "name is null");
        this.isRemote = checkNotNull(isRemote, "isRemote is null");

        if (isRemote) {
            String[] parts = name.split(":");
            checkArgument(parts.length == 2 && Ints.tryParse(parts[1]) != null,
                          "remote name should be host:port, but was " + name);

            remoteHost = parts[0];
            remotePort = Ints.tryParse(parts[1]);
        }
    }

    public String getName() {
        return name;
    }

    public boolean isRemote() {
        return isRemote;
    }

    @JsonIgnore
    public String getRemoteHost() {
        return remoteHost;
    }

    @JsonIgnore
    public int getRemotePort() {
        return remotePort;
    }

    @Override
    public String toString() {
        if (isRemote) {
            return "RemoteEndpoint(" + name + ")";
        }
        return "LocalEndpoint(" + name + ")";
    }
}
