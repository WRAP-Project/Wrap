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
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Schedule not found."),
    AVAILABILITY_REQUEST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AVAILABILITY_REQUEST_NOT_FOUND",
            "Availability request not found."
    ),
    AVAILABILITY_RESPONSE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AVAILABILITY_RESPONSE_NOT_FOUND",
            "Availability response not found."
    ),
    INVALID_AVAILABILITY_RANGE(
            HttpStatus.BAD_REQUEST,
            "INVALID_AVAILABILITY_RANGE",
            "Invalid availability request range."
    ),
    INVALID_SLOT_RANGE(HttpStatus.BAD_REQUEST, "INVALID_SLOT_RANGE", "Invalid availability slot range."),
    INVALID_SLOT_UNIT(HttpStatus.BAD_REQUEST, "INVALID_SLOT_UNIT", "Invalid availability slot unit."),
    INVALID_RECOMMENDED_SLOT(
            HttpStatus.BAD_REQUEST,
            "INVALID_RECOMMENDED_SLOT",
            "Selected slot is not available for every project member."
    ),
    AVAILABILITY_REQUEST_DUPLICATED(
            HttpStatus.CONFLICT,
            "AVAILABILITY_REQUEST_DUPLICATED",
            "Availability request already exists for the same range."
    ),
    AVAILABILITY_REQUEST_ALREADY_CONFIRMED(
            HttpStatus.CONFLICT,
            "AVAILABILITY_REQUEST_ALREADY_CONFIRMED",
            "Availability request is already confirmed."
    ),
    AVAILABILITY_REQUEST_CANCELED(
            HttpStatus.CONFLICT,
            "AVAILABILITY_REQUEST_CANCELED",
            "Availability request is canceled."
    ),
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
