package com.wrap.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."),
    PROJECT_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "PROJECT_ACCESS_DENIED",
            "프로젝트에 접근할 권한이 없습니다."
    ),
    PROJECT_OWNER_REQUIRED(
            HttpStatus.FORBIDDEN,
            "PROJECT_OWNER_REQUIRED",
            "프로젝트 OWNER 권한이 필요합니다."
    ),
    PROJECT_ALREADY_COMPLETED(
            HttpStatus.CONFLICT,
            "PROJECT_ALREADY_COMPLETED",
            "이미 완료된 프로젝트입니다."
    ),
    PROJECT_NOT_COMPLETED(
            HttpStatus.CONFLICT,
            "PROJECT_NOT_COMPLETED",
            "완료된 프로젝트만 재진행할 수 있습니다."
    ),
    INVALID_PROJECT_DATE(
            HttpStatus.BAD_REQUEST,
            "INVALID_PROJECT_DATE",
            "프로젝트 시작일은 종료일보다 늦을 수 없습니다."
    ),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
