package com.meituan.data.jmxtools.jmx.jvm;

import com.meituan.data.jmxtools.jmx.Metric;

import java.util.Collection;

interface Collector {
    Collection<Metric> collect(String metricPrefix);
}
