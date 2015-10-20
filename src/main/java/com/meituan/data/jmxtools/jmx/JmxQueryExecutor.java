package com.meituan.data.jmxtools.jmx;

import com.meituan.data.jmxtools.conf.AttributeConf;
import com.meituan.data.jmxtools.conf.MBeanQuery;
import com.meituan.data.jmxtools.reporter.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.meituan.data.jmxtools.reporter.Metric.Type;

/**
 * This class is responsible for executing JMX query against a local/remote JMX endpoint.
 *
 * <p>Note that this class is not responsible for closing the connection.
 */
public class JmxQueryExecutor {
    static final Logger LOG = LoggerFactory.getLogger(JmxQueryExecutor.class);

    private MBeanServerConnection mBeanServer;

    /**
     * Construct a JMXQueryExecutor using a given JMX connection.
     *
     * @param connection a valid/connected JMX connection
     * @throws IOException if a valid
     * <code>MBeanServerConnection</code> cannot be created, for
     * instance because the connection to the remote MBean server has
     * not yet been established, or it has been closed, or it has broken.
     */
    public JmxQueryExecutor(JMXConnector connection) throws IOException {
        checkNotNull(connection, "connection is null");
        this.mBeanServer = connection.getMBeanServerConnection();
    }

    /**
     * Execute the given mbean query.
     * @param query the MBean and it's attributes to query
     * @return metric list
     * @throws JmxQueryException when failed to get MBean attribute from MBeanServer
     */
    public List<Metric> execute(MBeanQuery query) throws JmxQueryException {
        List<Metric> result = new ArrayList<>();
        Object attr;

        outer:
        for (AttributeConf attrConf : query.getAttributes()) {
            String[] attrNames = attrConf.getName().split("\\.");
            int i = 0;
            // metric key format: groupName.attr0.attr1
            StringBuilder key = new StringBuilder(query.getGroupName());

            try {
                attr = mBeanServer.getAttribute(new ObjectName(query.getObjectName()), attrNames[i]);
            } catch (Exception e) {
                throw new JmxQueryException("Failed to get " + key + "." + attrNames[i] + " from MBean server", e);
            }

            // walk down the object graph according to `attrNames`
            while (i < attrNames.length - 1) { // not leaf attr
                key.append(".").append(attrNames[i]);
                i++;

                // note: only permit dive into nested composite data so far
                if (!(attr instanceof CompositeData)) {
                    LOG.warn("{} isn't of type CompositeData: {}", key, attr);
                    continue outer;
                }
                CompositeData compData = (CompositeData) attr;
                CompositeType compType = compData.getCompositeType();
                if (!compType.containsKey(attrNames[i])) {
                    LOG.warn("{} not found!", key + "." + attrNames[i]);
                    continue outer;
                }
                attr = compData.get(attrNames[i]);
            }

            key.append(".").append(attrNames[i]);
            // get value from the leaf attribute
            if (!(attr instanceof Number)) {
                LOG.warn("{} isn't of type Number: {}", attr);
                continue;
            }

            final Type metricType = Type.of(attrConf.getType());
            final Metric metric = new Metric(key.toString(), (Number) attr, metricType);
            result.add(metric);
        }

        return result;
    }

    /**
     * Execute all queries and concatenate their results.
     * @param queries
     * @return metric list
     * @throws JmxQueryException when failed to get MBean attribute from MBeanServer
     */
    public List<Metric> executeAll(List<MBeanQuery> queries) throws JmxQueryException {
        List<Metric> result = new ArrayList<>();

        for (MBeanQuery query : queries) {
            result.addAll(execute(query));
        }

        return result;
    }
}
