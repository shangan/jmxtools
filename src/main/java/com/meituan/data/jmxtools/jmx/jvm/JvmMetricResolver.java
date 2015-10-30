package com.meituan.data.jmxtools.jmx.jvm;

import com.google.common.collect.Lists;
import com.meituan.data.jmxtools.conf.JvmMetricGroup;
import com.meituan.data.jmxtools.jmx.Metric;
import com.meituan.data.jmxtools.jmx.MetricResolver;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.*;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class JvmMetricResolver extends MetricResolver<JvmMetricGroup> {

    public JvmMetricResolver(MBeanServerConnection connection) {
        super(connection);
    }

    @Override
    public Collection<Metric> resolve(JvmMetricGroup metricGroup) throws IOException {
        checkNotNull(metricGroup, "metricGroup is null");

        Collection<Metric> result = Lists.newArrayList();
        String prefix = metricGroup.getGroupName() + ".";

        List<Collector> collectors = Lists.newArrayList(
                new MemoryCollector(proxy(MemoryMXBean.class), proxies(MemoryPoolMXBean.class)),
                new GcCollector(proxies(GarbageCollectorMXBean.class)),
                new ClassLoadingCollector(proxy(ClassLoadingMXBean.class)),
                new ThreadCollector(proxy(ThreadMXBean.class)),
                new OSCollector(proxy(com.sun.management.UnixOperatingSystemMXBean.class))
        );

        for (Collector collector : collectors) {
            result.addAll(collector.collect(prefix));
        }

        return result;
    }

    private <T extends PlatformManagedObject> T proxy(Class<T> mbeanInterface) throws IOException {
        return ManagementFactory.getPlatformMXBean(connection, mbeanInterface);
    }

    private <T extends PlatformManagedObject> List<T> proxies(Class<T> mbeanInterface) throws IOException {
        return ManagementFactory.getPlatformMXBeans(connection, mbeanInterface);
    }
}
