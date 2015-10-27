package com.meituan.data.jmxtools.jmx;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

public class Metric {
    public enum Type {
        GAUGE, COUNTER;

        // static factory method that is case-insensitive
        public static Type of(String s) {
            checkNotNull(s);
            return Type.valueOf(s.toUpperCase());
        }
    }

    private final String name;
    private final Number value;
    private final Type type;

    public Metric(String name, Number value, Type type) {
        this.name = checkNotNull(name);
        this.value = checkNotNull(value);
        this.type = checkNotNull(type);
    }

    public String getName() {
        return name;
    }

    public Number getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("value", value)
                .add("type", type)
                .toString();
    }
}
