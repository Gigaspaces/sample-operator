package com.gigaspaces.k8s.operators;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapBuilder<K, V> {
    final HashMap<K, V> map = new HashMap<>();

    public static <K,V> Map<K,V> singletonMap(K key, V value) {
        return Collections.singletonMap(key, value);
    }

    public MapBuilder<K, V> put(K k, V v) {
        map.put(k, v);
        return this;
    }

    public Map<K, V> build() {
        return map;
    }
}
