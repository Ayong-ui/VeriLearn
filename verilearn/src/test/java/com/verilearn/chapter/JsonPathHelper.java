package com.verilearn.chapter;

import com.jayway.jsonpath.JsonPath;

public final class JsonPathHelper {

    private JsonPathHelper() {
    }

    public static Long readLong(String json, String path) {
        Number value = JsonPath.read(json, path);
        return value == null ? null : value.longValue();
    }
}
