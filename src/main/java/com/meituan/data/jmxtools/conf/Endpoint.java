package com.meituan.data.jmxtools.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.meituan.data.jmxtools.utils.Preconditions.checkArgument;
import static com.meituan.data.jmxtools.utils.Preconditions.checkNotNull;

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

    static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @JsonCreator
    public Endpoint(@JsonProperty("name") String name,
                    @JsonProperty("remote") boolean isRemote) {
        this.name = checkNotNull(name, "name is null");
        this.isRemote = checkNotNull(isRemote, "isRemote is null");

        if (isRemote) {
            String[] parts = name.split(":");
            checkArgument(parts.length == 2 && tryParseInt(parts[1]) != null,
                          "remote name should be host:port, but was " + name);

            remoteHost = parts[0];
            remotePort = Integer.parseInt(parts[1]);
        }
    }

    public String getName() {
        return name;
    }

    public boolean isRemote() {
        return isRemote;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

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
