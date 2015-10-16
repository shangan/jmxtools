package com.meituan.data.jmxtools.reporter;

public class MetricsReportException extends Exception {
    public MetricsReportException(String message) {
        super(message);
    }

    public MetricsReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
