package com.meituan.data.jmxtools.jmx.jvm;

import com.google.common.collect.Lists;
import com.meituan.data.jmxtools.jmx.Metric;

import java.lang.management.ClassLoadingMXBean;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.meituan.data.jmxtools.jmx.Metric.Type.GAUGE;

/**
 * ClassLoadingCollector produces the following metrics:
 *
 * <ul>
 *     <li>ClassLoading.Loaded</li>
 *     <li>ClassLoading.TotalLoaded</li>
 *     <li>ClassLoading.TotalUnloaded</li>
 * </ul>
 */
class ClassLoadingCollector implements Collector {
    private final ClassLoadingMXBean classLoadingMXBean;

    public ClassLoadingCollector(ClassLoadingMXBean classLoadingMXBean) {
        this.classLoadingMXBean = checkNotNull(classLoadingMXBean, "classLoadingMXBean is null");
    }

    @Override
    public Collection<Metric> collect(String metricPrefix) {
        Collection<Metric> metrics = Lists.newArrayList();
        metricPrefix += "ClassLoading.";

        metrics.add(new Metric(metricPrefix + "Loaded", classLoadingMXBean.getLoadedClassCount(), GAUGE));
        metrics.add(new Metric(metricPrefix + "TotalLoaded", classLoadingMXBean.getTotalLoadedClassCount(), GAUGE));
        metrics.add(new Metric(metricPrefix + "TotalUnloaded", classLoadingMXBean.getUnloadedClassCount(), GAUGE));

        return metrics;
    }
}
