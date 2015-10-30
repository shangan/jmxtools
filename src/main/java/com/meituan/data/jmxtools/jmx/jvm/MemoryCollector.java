package com.meituan.data.jmxtools.jmx.jvm;

import com.google.common.collect.Lists;
import com.meituan.data.jmxtools.jmx.Metric;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.meituan.data.jmxtools.jmx.Metric.Type.GAUGE;

/**
 * MemoryCollector will produce the following kinds of metrics:
 *
 * <ul>
 *     <li>Mem.{Heap, NonHeap}.{Used, Committed, Max}[.Percent]</li>
 *     <li>Mem.{Young, Old}.{Used, Committed, Max}[.Percent]</li>
 *     <li>MemPool.{mem pool name}.{Used, Committed, Max}[.Percent]</li>
 * </ul>
 */
class MemoryCollector implements Collector {

    // Since Java uses generational garbage collection, people want to know
    // young & old memory usage of java heap
    enum HeapMemType {
        Young {
            @Override
            boolean include(String poolName) {
                return poolName.contains("Eden Space") || poolName.contains("Survivor Space");
            }
        },
        Old {
            @Override
            boolean include(String poolName) {
                return poolName.contains("Tenured Gen") || poolName.contains("Old Gen");
            }
        };

        abstract boolean include(final String poolName);
    }

    // java.lang.management.MemoryUsage use -1 for undefined value
    private static long MEM_SIZE_UNDEFINED = -1;

    private final MemoryMXBean memoryMXBean;
    private final List<MemoryPoolMXBean> memoryPoolMXBeans;

    public MemoryCollector(MemoryMXBean memoryMXBean, List<MemoryPoolMXBean> memoryPoolMXBeans) {
        this.memoryMXBean = checkNotNull(memoryMXBean, "memoryMXBean is null");
        this.memoryPoolMXBeans = checkNotNull(memoryPoolMXBeans, "memoryPoolMXBeans is null");
    }

    @Override
    public Collection<Metric> collect(String metricPrefix) {
        Collection<Metric> metrics = Lists.newArrayList();

        collectMemoryUsage(metricPrefix + "Mem.Heap.", memoryMXBean.getHeapMemoryUsage(), metrics);
        collectMemoryUsage(metricPrefix + "Mem.NonHeap.", memoryMXBean.getNonHeapMemoryUsage(), metrics);

        MemoryUsage youngUsage = new MemoryUsage(0, 0, 0, 0);
        MemoryUsage oldUsage = new MemoryUsage(0, 0, 0, 0);

        // mem pool specific usage
        for (MemoryPoolMXBean memPoolMXBean : memoryPoolMXBeans) {
            String newPoolName = memPoolMXBean.getName().replaceAll("\\s", "");
            MemoryUsage poolUsage = memPoolMXBean.getUsage();

            collectMemoryUsage(metricPrefix + "MemPool." + newPoolName + ".", poolUsage, metrics);

            if (HeapMemType.Young.include(memPoolMXBean.getName())) {
                youngUsage = mergeMemoryUsage(youngUsage, poolUsage);

            } else if (HeapMemType.Old.include(memPoolMXBean.getName())) {
                oldUsage = mergeMemoryUsage(oldUsage, poolUsage);
            }
        }

        // young / old usage
        collectMemoryUsage(metricPrefix + "Mem.Young.", youngUsage, metrics);
        collectMemoryUsage(metricPrefix + "Mem.Old.", oldUsage, metrics);

        return metrics;
    }

    private MemoryUsage mergeMemoryUsage(MemoryUsage p, MemoryUsage c) {
        long init = p.getInit() + c.getInit();
        if (p.getInit() == MEM_SIZE_UNDEFINED || c.getInit() == MEM_SIZE_UNDEFINED) {
            init = MEM_SIZE_UNDEFINED;
        }

        long max = p.getMax() + c.getMax();
        if (p.getMax() == MEM_SIZE_UNDEFINED || c.getMax() == MEM_SIZE_UNDEFINED) {
            max = MEM_SIZE_UNDEFINED;
        }

        return new MemoryUsage(
                init,
                p.getUsed() + c.getUsed(),
                p.getCommitted() + c.getCommitted(),
                max);
    }

    private void collectMemoryUsage(String prefix, MemoryUsage usage, Collection<Metric> metrics) {
        metrics.add(new Metric(prefix + "Used", usage.getUsed(), GAUGE));
        metrics.add(new Metric(prefix + "Committed", usage.getCommitted(), GAUGE));
        metrics.add(new Metric(prefix + "Max", usage.getMax(), GAUGE));

        // it makes no sense to compute usage percent if the memory pool doesn't have a max value.
        // e.g., in java 8, the Metaspace pool doesn't have a max unless specified explicitly
        if (usage.getMax() != MEM_SIZE_UNDEFINED) {
            double usedPercent = 100.0 * usage.getUsed() / usage.getMax();
            metrics.add(new Metric(prefix + "Used.Percent", usedPercent, GAUGE));
        }
    }
}
