package com.gigaspaces.k8s.operators;

import java.util.*;

public class ListBuilder<E> {
    final List<E> list = new ArrayList<>();

    public static <E> List<E> singletonList(E e) {
        return Collections.singletonList(e);
    }

    public ListBuilder<E> add(E e) {
        list.add(e);
        return this;
    }

    public List<E> build() {
        return list;
    }
}
