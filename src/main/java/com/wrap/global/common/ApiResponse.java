package com.wrap.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private boolean success;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ErrorBody error;

    public static <T> ApiResponse<T> success(T data, String message) { return new ApiResponse<>(true, data, message, null);}

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, null, message, null);
    }

    public static <T> ApiResponse<T> fail(ErrorBody error) {
        return new ApiResponse<>(false, null, null, error);
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor
    public static class ErrorBody {
        private String code;
        private String message;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private java.util.List<FieldError> details;
    }

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String reason;
    }
}
