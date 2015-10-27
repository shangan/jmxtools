package com.meituan.data.jmxtools.conf;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MetricGroupTest extends ConfTestBase {

    @Test
    public void testGlobMetricGroup() throws IOException {
        GlobMetricGroup simple = fromFile("glob_metric_group_simple.json", GlobMetricGroup.class);
        assertEquals("QueryManager", simple.getGroupName());
        assertEquals("com.facebook.presto.execution:name=QueryManager", simple.getObjectNameString());
        assertEquals(Lists.newArrayList("*"), simple.getGauges());
        assertTrue(simple.getCounters().isEmpty());

        GlobMetricGroup full = fromFile("glob_metric_group_full.json", GlobMetricGroup.class);
        assertEquals("QueryManager", full.getGroupName());
        assertEquals("com.facebook.presto.execution:name=QueryManager", full.getObjectNameString());

        assertEquals(Lists.newArrayList("RunningQueries", "StartedQueries.*.Count"),
                full.getGauges());

        assertEquals(Lists.newArrayList("StartedQueries.TotalCount", "CompletedQueries.TotalCount"),
                full.getCounters());
    }

    @Test
    public void testCustomMetricGroup() throws IOException {
        CustomMetricGroup group = fromFile("custom_metric_group.json", CustomMetricGroup.class);
        assertEquals("com.meituan.data.jmxtools.conf.TestCustomResolver", group.getResolverClassName());
        assertEquals(2, group.getConfig().size());

        // TODO run group.resolveMetrics
    }
}