package com.meituan.data.jmxtools.reporter;

import com.meituan.data.jmxtools.conf.ReporterConf;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory for {@link Reporter}.
 */
public final class Reporters {
    private Reporters() {}

    /**
     * Return a new reporter according to `conf`.
     * @param conf specify which reporter to use and its configurations
     * @return a new reporter
     */
    public static Reporter newReporter(ReporterConf conf) {
        checkNotNull(conf, "conf is null");

        if (conf.getType().equals("falcon")) {
            return new FalconReporter(conf.getOptions());
        }
        throw new IllegalArgumentException("Unsupported reporter type: " + conf.getType());
    }
}
