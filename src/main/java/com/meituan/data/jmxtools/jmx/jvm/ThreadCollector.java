package com.meituan.data.jmxtools.jmx.jvm;

import com.google.common.collect.Lists;
import com.meituan.data.jmxtools.jmx.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.meituan.data.jmxtools.jmx.Metric.Type.GAUGE;

/**
 * ThreadCollector will produce the following metrics:
 * <ul>
 *     <li>Thread.LiveCount</li>
 *     <li>Thread.DaemonCount</li>
 *     <li>Thread.PeakCount</li>
 *     <li>Thread.TotalStartedCount</li>
 *     <li>Thread.DeadlockCount</li>
 *     <li>Thread.{Runnable, Blocked, Waiting}.Count</li>
 * </ul>
 */
class ThreadCollector implements Collector {
    private static Logger LOG = LoggerFactory.getLogger(ThreadCollector.class);
    private final ThreadMXBean threadMXBean;

    public ThreadCollector(ThreadMXBean threadMXBean) {
        this.threadMXBean = checkNotNull(threadMXBean, "threadMXBean is null");
    }

    @Override
    public Collection<Metric> collect(String metricPrefix) {
        Collection<Metric> metrics = Lists.newArrayList();
        metricPrefix += "Thread.";

        metrics.add(new Metric(metricPrefix + "LiveCount", threadMXBean.getThreadCount(), GAUGE));
        metrics.add(new Metric(metricPrefix + "DaemonCount", threadMXBean.getDaemonThreadCount(), GAUGE));
        metrics.add(new Metric(metricPrefix + "PeakCount", threadMXBean.getPeakThreadCount(), GAUGE));
        metrics.add(new Metric(metricPrefix + "TotalStartedCount", threadMXBean.getTotalStartedThreadCount(), GAUGE));

        try {
            long[] deadlocked = threadMXBean.findDeadlockedThreads();
            int deadlockedCount = (deadlocked == null) ? 0 : deadlocked.length;
            metrics.add(new Metric(metricPrefix + "DeadlockCount", deadlockedCount, GAUGE));

        } catch (UnsupportedOperationException e) {
            LOG.debug("findDeadlockedThreads is not supported", e);
        }

        int runnableCount = 0;
        int blockedCount= 0;
        int waitingCount = 0;
        long[] allLiveIds = threadMXBean.getAllThreadIds();

        for (ThreadInfo info : threadMXBean.getThreadInfo(allLiveIds)) {
            if (info != null) {
                switch (info.getThreadState()) {
                    case RUNNABLE:
                        runnableCount++;
                        break;
                    case BLOCKED:
                        blockedCount++;
                        break;
                    case WAITING:
                        waitingCount++;
                        break;
                }
            }
        }

        metrics.add(new Metric(metricPrefix + "Runnable.Count", runnableCount, GAUGE));
        metrics.add(new Metric(metricPrefix + "Blocked.Count", blockedCount, GAUGE));
        metrics.add(new Metric(metricPrefix + "Waiting.Count", waitingCount, GAUGE));

        return metrics;
    }
}
