package com.wrap.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "Invalid date range."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "Email or password is invalid."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_MEMBER_NOT_FOUND", "Project member not found."),
    PROJECT_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "PROJECT_ACCESS_DENIED",
            "Project access is denied."
    ),
    PROJECT_OWNER_REQUIRED(
            HttpStatus.FORBIDDEN,
            "PROJECT_OWNER_REQUIRED",
            "Project owner permission is required."
    ),
    PROJECT_ALREADY_COMPLETED(
            HttpStatus.CONFLICT,
            "PROJECT_ALREADY_COMPLETED",
            "Project is already completed."
    ),
    PROJECT_NOT_COMPLETED(
            HttpStatus.CONFLICT,
            "PROJECT_NOT_COMPLETED",
            "Only completed projects can be reopened."
    ),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Schedule not found."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email already exists."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
