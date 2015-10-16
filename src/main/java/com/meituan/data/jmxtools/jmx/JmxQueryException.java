package com.meituan.data.jmxtools.jmx;

public class JmxQueryException extends Exception {
    public JmxQueryException(String message) {
        super(message);
    }

    public JmxQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public JmxQueryException(Throwable cause) {
        super(cause);
    }

    public JmxQueryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
