package com.meituan.data.jmxtools.jmx;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.meituan.data.jmxtools.conf.GlobMetricGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.meituan.data.jmxtools.jmx.Metric.Type.COUNTER;
import static com.meituan.data.jmxtools.jmx.Metric.Type.GAUGE;

/**
 * A metric resolver using file system glob-like pattern to match metrics.
 *
 * <p>In the pattern, '*' means any number of any characters, '?' means any one character,
 * all other character will match itself. Also you use '\\' to escape '*' and '?'.
 *
 * <p>For example, "A.*.C" will match "A.BBB.C" and "A.BB.BB.C", pattern
 * "A.B?" will match "A.B1" and "A.B2" but not "A.BBB".
 *
 * <p>All metrics are collected from the leaf attribute of a managed bean. If the attribute has
 * nested structure, we go from top to bottom and using '.' to concatenate name. For array type
 * attribute, its children are not visited.
 *
 * For example, the following MBean will produce three metrics: "c1", "c2.c3", "c2.c4".
 * <pre>
 * {
 *     "ObjectName": "SampleObject",
 *     "c1": 1,
 *     "c2": {
 *         "c3": 2,
 *         "c4": 3
 *     },
 *     "c5": [{ // ignored
 *         "c6": 4
 *     }]
 * }
 * </pre>
 *
 * You can specify patterns for both <b>gauge</b> and <b>counter</b> metrics. To avoid
 * duplicated name problem when you want a metric to be both gauge and counter, a
 * ".Delta" postfix is added to all counter metrics for you.
 *
 * For example, if you use the following config to collect metrics for the above MBean,
 * <pre>
 * {
 *   "group": "Sample",
 *   "objectName": "SampleObject",
 *   "gauges": ["c1", "c2.*"],
 *   "counters": ["c1"]
 * }
 * </pre>
 *
 * The result metrics are
 * <ul>
 *     <li>Sample.c1</li>
 *     <li>Sample.c1.Delta</li>
 *     <li>Sample.c2.c3</li>
 *     <li>Sample.c2.c4</li>
 * </ul>
 */
public class GlobMetricResolver extends MetricResolver<GlobMetricGroup> {
    static final Logger LOG = LoggerFactory.getLogger(GlobMetricResolver.class);

    public GlobMetricResolver(MBeanServerConnection connection) {
        super(connection);
    }

    @Override
    public Collection<Metric> resolve(GlobMetricGroup metricGroup) throws IOException {
        checkNotNull(connection, "connection is null");
        checkNotNull(metricGroup, "metricGroup is null");

        Map<String, Number> allMetrics = fetchAllMetrics(metricGroup.getObjectName());

        Map<String, Number> gauges = extract(allMetrics, metricGroup.getGauges());

        Map<String, Number> counters = extract(allMetrics, metricGroup.getCounters());

        // Merge gauges and counters.
        // all counter's name has a ".Delta" postfix (assuming no gauge has that postfix)
        Map<String, Metric> result = Maps.newTreeMap();
        final String prefix = metricGroup.getGroupName() + ".";
        final String postfix = ".Delta";

        for (String name : gauges.keySet()) {
            String key = prefix + name;
            result.put(key, new Metric(key, gauges.get(name), GAUGE));
        }
        for (String name : counters.keySet()) {
            String key = prefix + name + postfix;
            result.put(key, new Metric(key, counters.get(name), COUNTER));
        }

        return result.values();
    }

    private Map<String, Number> fetchAllMetrics(ObjectName objectName) throws IOException {
        Map<String, Number> result = Maps.newTreeMap();

        MBeanInfo info;
        try {
            info = connection.getMBeanInfo(objectName);

        } catch (InstanceNotFoundException | IntrospectionException | ReflectionException e) {
            // InstanceNotFoundException: Ignored for some reason the bean was not found
            // IntrospectionException: Something odd happened with reflection
            // ReflectionException: Happens when the code inside the JMX bean threw an exception
            // In all above cases, we log it and skip processing this MBean
            LOG.warn("Problem occurred while trying to process MBean: " + objectName, e);
            return result;
        }

        for (MBeanAttributeInfo attrInfo : info.getAttributes()) {
            if (attrInfo.isReadable()) {
                String attrName = attrInfo.getName();
                try {
                    Object attrValue = connection.getAttribute(objectName, attrName);
                    dfsTraverse(attrName, attrValue, result);

                } catch (JMException | JMRuntimeException e) {
                    // JMException and JMRuntimeException are thrown by JMX implementations.
                    // In these cases, we just log it and keep processing other attributes
                    if (e.getCause() instanceof UnsupportedOperationException) {
                        // we don't want to log UnsupportedOperationException as warn level or higher
                        LOG.debug("getting attribute " + attrName + " of " + objectName + " threw an exception", e);
                    } else {
                        LOG.warn("getting attribute " + attrName + " of " + objectName + " threw an exception", e);
                    }
                } catch (RuntimeException e) {
                    // For some reason Runtime exceptions can still find their way through
                    LOG.error("getting attribute " + attrName + " of " + objectName + " threw an exception", e);
                }
            }
        }

        return result;
    }

    private void dfsTraverse(String prefix, Object value, Map<String, Number> result) {
        if (value == null) {
            return;
        }

        if (value instanceof Number) {
            result.put(prefix, (Number) value);

        } else if (value instanceof CompositeData) {
            CompositeData compoValue = (CompositeData) value;
            for (String key : compoValue.getCompositeType().keySet()) {
                dfsTraverse(prefix + "." + key, compoValue.get(key), result);
            }
        }
    }

    @VisibleForTesting
    static class GlobMatcher implements Predicate<String> {
        private String pattern;

        public GlobMatcher(String pattern) {
            this.pattern = checkNotNull(pattern, "pattern is null");
        }

        @Override
        public boolean apply(String input) {
            checkNotNull(input, "input is null");

            if (!pattern.contains("*") && !pattern.contains("?")) {
                return pattern.equals(input);   // fall back to exact matching
            }
            return input.matches(convertGlobToRegex(pattern));
        }

        @VisibleForTesting
        static String convertGlobToRegex(String glob) {
            StringBuilder sb = new StringBuilder("^");
            boolean escape = false;

            for (char c : glob.toCharArray()) {
                switch (c) {
                    case '*':
                        sb.append(escape ? "\\*" : ".*");
                        break;
                    case '?':
                        sb.append(escape ? "\\?" : ".");
                        break;
                    case '\\':
                        if (escape) {
                            sb.append("\\\\");
                        }
                        break;
                    case '.':
                    case '(':
                    case ')':
                    case '{':
                    case '}':
                    case '+':
                    case '|':
                    case '^':
                    case '$':
                    case '@':
                    case '%':
                        if (escape) {
                            sb.append("\\\\");
                        }
                        sb.append("\\").append(c);
                        break;
                    default:
                        if (escape) {
                            sb.append("\\\\");
                        }
                        sb.append(c);
                }
                escape = !escape && c == '\\';
            }

            if (escape) {
                sb.append("\\\\");
            }
            sb.append("$");

            return sb.toString();
        }
    }

    Map<String, Number> extract(Map<String, Number> metrics, List<String> globs) {
        List<GlobMatcher> matchers = Lists.transform(globs, new Function<String, GlobMatcher>() {
            @Override
            public GlobMatcher apply(String pattern) {
                return new GlobMatcher(pattern);
            }
        });

        return Maps.filterKeys(metrics, Predicates.or(matchers));
    }
}
