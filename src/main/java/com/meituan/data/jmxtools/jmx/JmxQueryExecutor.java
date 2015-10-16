package com.meituan.data.jmxtools.jmx;

import com.meituan.data.jmxtools.conf.Endpoint;
import com.meituan.data.jmxtools.conf.MBeanQuery;
import com.meituan.data.jmxtools.utils.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.remote.JMXConnector;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.meituan.data.jmxtools.utils.Preconditions.checkNotNull;

/**
 * This class is responsible for executing JMX query against a local/remote JMX endpoint.
 */
public class JmxQueryExecutor implements Closeable {
    static final Logger LOG = LoggerFactory.getLogger(JmxQueryExecutor.class);
    private static final long JMX_CONNECT_TIMEOUT_SEC = 20;

    private Endpoint endpoint;
    private JMXConnector connector;
    private MBeanServerConnection mBeanServer;

    public JmxQueryExecutor(Endpoint endpoint) throws IOException {
        this.endpoint = checkNotNull(endpoint, "endpoint is null");
        this.connector = JmxConnections.connectWithTimeout(endpoint, JMX_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS);
        this.mBeanServer = this.connector.getMBeanServerConnection();
    }

    /**
     * Execute the given mbean query.
     * @param query the MBean and it's attributes to query
     * @return metric list, with each metric represented by tuple (metric_name, metric_value).
     * @throws JmxQueryException when failed to get MBean attribute from MBeanServer
     */
    public List<Tuple2<String, Number>> execute(MBeanQuery query) throws JmxQueryException {
        List<Tuple2<String, Number>> result = new ArrayList<>();
        Object attr;

        outer:
        for (List<String> attrNames : query.getAttributes()) {
            if (attrNames.isEmpty()) continue;

            int i = 0;
            // metric key format: queryName.attr0.attr1
            StringBuilder key = new StringBuilder(query.getName());

            try {
                attr = mBeanServer.getAttribute(new ObjectName(query.getObjectName()), attrNames.get(i));
            } catch (Exception e) {
                throw new JmxQueryException("Failed to get " + key + "." + attrNames.get(i) + " from MBean server", e);
            }

            // walk down the object graph according to `attrNames`
            while (i < attrNames.size() - 1) { // not leaf attr
                key.append(".").append(attrNames.get(i));
                i++;

                // note: only permit dive into nested composite data so far
                if (!(attr instanceof CompositeData)) {
                    LOG.warn("{} isn't of type CompositeData: {}", key, attr);
                    continue outer;
                }
                CompositeData compData = (CompositeData) attr;
                CompositeType compType = compData.getCompositeType();
                if (!compType.containsKey(attrNames.get(i))) {
                    LOG.warn("{} not found!", key + "." + attrNames.get(i));
                    continue outer;
                }
                attr = compData.get(attrNames.get(i));
            }

            key.append(".").append(attrNames.get(i));
            // get value from the leaf attribute
            if (!(attr instanceof Number)) {
                LOG.warn("{} isn't of type Number: {}", attr);
                continue;
            }
            result.add(new Tuple2<>(key.toString(), (Number) attr));
        }

        return result;
    }

    /**
     * Execute all queries and concatenate their results.
     * @param queries
     * @return metric list, with each metric represented by tuple (metric_name, metric_value).
     * @throws JmxQueryException when failed to get MBean attribute from MBeanServer
     */
    public List<Tuple2<String, Number>> executeAll(List<MBeanQuery> queries) throws JmxQueryException {
        List<Tuple2<String, Number>> result = new ArrayList<>();

        for (MBeanQuery query : queries) {
            result.addAll(execute(query));
        }

        return result;
    }

    public void close() {
        if (connector != null) {
            try {
                LOG.debug("Close JMX Connection to {}", endpoint);
                connector.close();
            } catch (IOException e) {
                LOG.error("Failed to close JMX connection", e);
            }
        }
    }
}
