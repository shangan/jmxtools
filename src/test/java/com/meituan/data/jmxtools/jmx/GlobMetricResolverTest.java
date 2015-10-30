package com.meituan.data.jmxtools.jmx;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.meituan.data.jmxtools.conf.GlobMetricGroup;
import com.meituan.data.jmxtools.jmx.Metric.Type;
import org.junit.Test;

import javax.management.MBeanServer;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.meituan.data.jmxtools.jmx.GlobMetricResolver.GlobMatcher.convertGlobToRegex;
import static org.junit.Assert.*;

public class GlobMetricResolverTest {

    @Test
    public void testConvertGlobToRegex() {
        assertEquals("^$", convertGlobToRegex(""));
        assertEquals("^.*$", convertGlobToRegex("*"));
        assertEquals("^A\\.B\\.C$", convertGlobToRegex("A.B.C"));
        assertEquals("^.*\\.B\\.C$", convertGlobToRegex("*.B.C"));
        assertEquals("^A\\..*\\.C$", convertGlobToRegex("A.*.C"));
        assertEquals("^A\\.B\\..*$", convertGlobToRegex("A.B.*"));
        assertEquals("^A\\..*\\..*$", convertGlobToRegex("A.*.*"));
        assertEquals("^.*\\..*\\..*$", convertGlobToRegex("*.*.*"));
        assertEquals("^A..\\.B..$", convertGlobToRegex("A??.B??"));
        assertEquals("^A\\..*\\.B..$", convertGlobToRegex("A.*.B??"));
        assertEquals("^A\\*\\.B\\?$", convertGlobToRegex("A\\*.B\\?"));
        assertEquals("^\"A\\.B\"$", convertGlobToRegex("\"A.B\""));
        assertEquals("^\\(A\\)\\.B\\{2\\}\\.C\\+$", convertGlobToRegex("(A).B{2}.C+"));
        assertEquals("^\\\\A\\\\\\.B\\\\\\{C\\\\\\}$", convertGlobToRegex("\\A\\.B\\{C\\}")); // YES, it's ugly!
    }

    private void match(String text, String pattern) {
        assertTrue(new GlobMetricResolver.GlobMatcher(pattern).apply(text));
    }

    private void notMatch(String text, String pattern) {
        assertFalse(new GlobMetricResolver.GlobMatcher(pattern).apply(text));
    }

    @Test
    public void testGlobMatcher() {
        final String text = "A.B.C";
        match(text, "A.B.C");
        match(text, "*.B.C");
        match(text, "A.*.C");
        match(text, "A.B.*");
        match(text, "A.*");
        match(text, "*.C");
        match(text, "A*C");
        match(text, "*");
        match(text, "A.B.?");
        match(text, "A.?.C");
        match(text, "A.B.?");
        match(text, "?.?.?");

        final String exactPattern = "A.B.C";
        notMatch("A.B", exactPattern);
        notMatch("A.B.D", exactPattern);
        notMatch("A.B.C.D", exactPattern);
        notMatch("AABBCC", exactPattern);

        final String wildCardPattern = "A.*.C";
        notMatch("B.C", wildCardPattern);
        notMatch("A.B", wildCardPattern);
        notMatch("ABC", wildCardPattern);
        notMatch("A.B.C.D", wildCardPattern);
    }

    @Test
    public void testResolve() throws IOException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        GlobMetricResolver resolver = new GlobMetricResolver(mBeanServer);

        List<String> gauges = Lists.newArrayList("HeapMemoryUsage.*", "NonHeapMemoryUsage.*");
        List<String> counters = Lists.newArrayList("HeapMemoryUsage.used", "NonHeapMemoryUsage.used");
        GlobMetricGroup group = new GlobMetricGroup("Mem", "java.lang:type=Memory", gauges, counters);

        Set<String> expectedGauges = Sets.newHashSet(
                "Mem.HeapMemoryUsage.committed",
                "Mem.HeapMemoryUsage.init",
                "Mem.HeapMemoryUsage.max",
                "Mem.HeapMemoryUsage.used",
                "Mem.NonHeapMemoryUsage.committed",
                "Mem.NonHeapMemoryUsage.init",
                "Mem.NonHeapMemoryUsage.max",
                "Mem.NonHeapMemoryUsage.used");

        Set<String> expectedCounters = Sets.newHashSet(
                "Mem.HeapMemoryUsage.used.Delta",
                "Mem.NonHeapMemoryUsage.used.Delta");

        Collection<Metric> metrics = resolver.resolve(group);
        Set<String> actualGauges = Sets.newHashSet();
        Set<String> actualCounters = Sets.newHashSet();

        for (Metric metric : metrics) {
            if (metric.getType() == Type.GAUGE) {
                actualGauges.add(metric.getName());
            } else if (metric.getType() == Type.COUNTER) {
                actualCounters.add(metric.getName());
            }
        }

        assertEquals(expectedGauges, actualGauges);
        assertEquals(expectedCounters, actualCounters);
    }
}