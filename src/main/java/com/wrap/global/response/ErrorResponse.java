package com.wrap.global.response;

import java.util.List;

public record ErrorResponse(
        boolean success,
        ErrorBody error
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(false, new ErrorBody(code, message, List.of()));
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> details) {
        return new ErrorResponse(false, new ErrorBody(code, message, details));
    }

    public record ErrorBody(
            String code,
            String message,
            List<FieldErrorDetail> details
    ) {
    }

    public record FieldErrorDetail(
            String field,
            String reason
    ) {
    }
}
