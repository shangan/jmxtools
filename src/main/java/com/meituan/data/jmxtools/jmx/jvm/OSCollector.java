package com.meituan.data.jmxtools.jmx.jvm;

import com.google.common.collect.Lists;
import com.meituan.data.jmxtools.jmx.Metric;
import com.sun.management.UnixOperatingSystemMXBean;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.meituan.data.jmxtools.jmx.Metric.Type.GAUGE;

/**
 * OSCollector will produce the following Operating System related metrics:
 * <ul>
 *     <li>OS.ProcessCpuLoad</li>
 *     <li>OS.ProcessCpuTimeMillis</li>
 *     <li>OS.SystemCpuLoad</li>
 *     <li>OS.OpenFileDescriptorCount</li>
 *     <li>OS.OpenFileDescriptorPercent</li>
 * </ul>
 */
class OSCollector implements Collector {
    private final UnixOperatingSystemMXBean unixOsMXBean;

    public OSCollector(UnixOperatingSystemMXBean unixOsMXBean) {
        this.unixOsMXBean = checkNotNull(unixOsMXBean, "unixOsMXBean is null");
    }

    @Override
    public Collection<Metric> collect(String metricPrefix) {
        Collection<Metric> metrics = Lists.newArrayList();
        metricPrefix += "OS.";

        metrics.add(new Metric(metricPrefix + "ProcessCpuLoad", unixOsMXBean.getProcessCpuLoad(), GAUGE));
        metrics.add(new Metric(metricPrefix + "ProcessCpuTimeMillis", unixOsMXBean.getProcessCpuTime() / 1e6, GAUGE));
        metrics.add(new Metric(metricPrefix + "SystemCpuLoad", unixOsMXBean.getSystemCpuLoad(), GAUGE));
        metrics.add(new Metric(metricPrefix + "OpenFileDescriptorCount", unixOsMXBean.getOpenFileDescriptorCount(), GAUGE));

        double fdPercent = 100.0 * unixOsMXBean.getOpenFileDescriptorCount() / unixOsMXBean.getMaxFileDescriptorCount();
        metrics.add(new Metric(metricPrefix + "OpenFileDescriptorPercent", fdPercent, GAUGE));

        return metrics;
    }
}
