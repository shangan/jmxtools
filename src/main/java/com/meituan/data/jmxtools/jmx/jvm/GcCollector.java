package com.meituan.data.jmxtools.jmx.jvm;

import com.google.common.collect.Lists;
import com.meituan.data.jmxtools.jmx.Metric;

import java.lang.management.GarbageCollectorMXBean;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.meituan.data.jmxtools.jmx.Metric.Type.COUNTER;
import static com.meituan.data.jmxtools.jmx.Metric.Type.GAUGE;

/**
 * GcCollector will produce the following GC-related gauge metrics and their <b>counter</b> version:
 * <ul>
 *     <li>GC.Minor.Count</li>
 *     <li>GC.Minor.TimeMillis</li>
 *     <li>GC.Major.Count</li>
 *     <li>GC.Major.TimeMillis</li>
 *     <li>GC.All.Count</li>
 *     <li>GC.All.TimeMillis</li>
 * </ul>
 */
class GcCollector implements Collector {

    enum GarbageCollectorType {
        Unknown, Minor, Major;

        private static Map<String, GarbageCollectorType> mapping;

        static {
            mapping = new HashMap<>();

            mapping.put("Copy", Minor);
            mapping.put("PS Scavenge", Minor);
            mapping.put("ParNew", Minor);
            mapping.put("G1 Young Generation", Minor);

            mapping.put("MarkSweepCompact", Major);
            mapping.put("PS MarkSweep", Major);
            mapping.put("ConcurrentMarkSweep", Major);
            mapping.put("G1 Old Generation", Major);
        }

        public static GarbageCollectorType of(String garbageCollectorName) {
            checkNotNull(garbageCollectorName, "garbageCollectorName is null");

            if (mapping.containsKey(garbageCollectorName)) {
                return mapping.get(garbageCollectorName);
            } else {
                return Unknown;
            }
        }
    }

    List<GarbageCollectorMXBean> garbageCollectorMXBeans;

    public GcCollector(List<GarbageCollectorMXBean> garbageCollectorMXBeans) {
        this.garbageCollectorMXBeans = checkNotNull(garbageCollectorMXBeans, "garbageCollectorMXBeans is null");
    }

    @Override
    public Collection<Metric> collect(String metricPrefix) {
        Collection<Metric> metrics = Lists.newArrayList();
        metricPrefix = metricPrefix + "GC.";

        long totalCount = 0;
        long totalTimeMillis = 0;

        for (GarbageCollectorMXBean gcMXBean : garbageCollectorMXBeans) {
            final long count = gcMXBean.getCollectionCount();
            final long time = gcMXBean.getCollectionTime();
            GarbageCollectorType gcType = GarbageCollectorType.of(gcMXBean.getName());

            addGaugeAndCounter(metricPrefix + gcType + ".Count", count, metrics);
            addGaugeAndCounter(metricPrefix + gcType + ".TimeMillis", time, metrics);

            totalCount += count;
            totalTimeMillis += time;
        }

        addGaugeAndCounter(metricPrefix + "All.Count", totalCount, metrics);
        addGaugeAndCounter(metricPrefix + "All.TimeMillis", totalTimeMillis, metrics);
        return metrics;
    }

    private void addGaugeAndCounter(String name, Number value, Collection<Metric> metrics) {
        metrics.add(new Metric(name, value, GAUGE));
        metrics.add(new Metric(name + ".Delta", value, COUNTER));
    }

}
