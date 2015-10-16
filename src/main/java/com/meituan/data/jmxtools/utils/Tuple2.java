package com.meituan.data.jmxtools.utils;

import static com.meituan.data.jmxtools.utils.Preconditions.checkNotNull;

public class Tuple2<T1, T2> {
    public T1 _1;
    public T2 _2;

    public Tuple2(T1 _1, T2 _2) {
        this._1 = checkNotNull(_1, "_1 is null");
        this._2 = checkNotNull(_2, "_2 is null");
    }

    @Override
    public String toString() {
        return "(" + _1 + ", " + _2 + ")";
    }
}
