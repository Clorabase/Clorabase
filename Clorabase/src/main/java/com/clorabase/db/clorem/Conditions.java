package com.clorabase.db.clorem;

import androidx.core.util.Predicate;

import java.util.List;
import java.util.Map;

public final class Conditions {

    public static Predicate<Map<String,Object>> whereEqual(String key, Object value) {
        return map -> value.equals(map.get(key));
    }

    public static Predicate<Map<String,Object>> whereNotEqual(String key, Object value) {
        return map -> !value.equals(map.get(key));
    }

    public static Predicate<Map<String,Object>> whereContains(String key, String value) {
        return map -> map.get(key) != null && ((String) map.get(key)).contains(value);
    }

    public static Predicate<Map<String,Object>> whereGreater(String key, Number value) {
        return map -> ((Number) map.get(key)).doubleValue() > value.doubleValue();
    }

    public static Predicate<Map<String,Object>> whereLess(String key, Number value) {
        return map -> ((Number) map.get(key)).doubleValue() < value.doubleValue();
    }

    public static Predicate<Map<String,Object>> whereIn(String key, List<Object> values) {
        return map -> values.contains(map.get(key));
    }
}
