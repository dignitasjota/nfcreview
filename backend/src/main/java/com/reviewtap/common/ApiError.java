package com.reviewtap.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Formato único de error REST. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, Map<String, String> fields) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}
