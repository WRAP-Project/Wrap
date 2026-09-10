package com.wrap.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값이 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "날짜 범위가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_MEMBER_NOT_FOUND", "프로젝트 멤버를 찾을 수 없습니다."),
    PROJECT_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "PROJECT_ACCESS_DENIED",
            "해당 프로젝트에 접근할 권한이 없습니다."
    ),
    PROJECT_OWNER_REQUIRED(
            HttpStatus.FORBIDDEN,
            "PROJECT_OWNER_REQUIRED",
            "프로젝트 관리자 권한이 필요합니다."
    ),
    PROJECT_ALREADY_COMPLETED(
            HttpStatus.CONFLICT,
            "PROJECT_ALREADY_COMPLETED",
            "이미 완료된 프로젝트입니다."
    ),
    PROJECT_NOT_COMPLETED(
            HttpStatus.CONFLICT,
            "PROJECT_NOT_COMPLETED",
            "완료된 프로젝트만 다시 진행할 수 있습니다."
    ),
    LAST_PROJECT_OWNER(
            HttpStatus.CONFLICT,
            "LAST_PROJECT_OWNER",
            "프로젝트에는 최소 한 명의 관리자가 필요합니다."
    ),
    PROJECT_OWNER_CANNOT_BE_REMOVED(
            HttpStatus.CONFLICT,
            "PROJECT_OWNER_CANNOT_BE_REMOVED",
            "프로젝트 관리자는 내보낼 수 없습니다."
    ),
    PROJECT_MEMBER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "PROJECT_MEMBER_ALREADY_EXISTS",
            "이미 프로젝트에 참여 중인 회원입니다."
    ),
    INVITATION_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "INVITATION_ALREADY_EXISTS",
            "이미 대기 중인 초대가 있습니다."
    ),
    INVITATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "INVITATION_NOT_FOUND",
            "초대를 찾을 수 없습니다."
    ),
    INVITATION_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "INVITATION_ACCESS_DENIED",
            "해당 초대에 접근할 권한이 없습니다."
    ),
    INVITATION_ALREADY_PROCESSED(
            HttpStatus.CONFLICT,
            "INVITATION_ALREADY_PROCESSED",
            "이미 처리된 초대입니다."
    ),
    INVITE_LINK_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "INVITE_LINK_ALREADY_EXISTS",
            "이미 활성화된 프로젝트 초대 링크가 있습니다."
    ),
    INVITE_LINK_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "INVITE_LINK_NOT_FOUND",
            "프로젝트 초대 링크를 찾을 수 없습니다."
    ),
    INVITE_LINK_ALREADY_REVOKED(
            HttpStatus.CONFLICT,
            "INVITE_LINK_ALREADY_REVOKED",
            "이미 비활성화된 프로젝트 초대 링크입니다."
    ),
    INVITE_LINK_TOKEN_GENERATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INVITE_LINK_TOKEN_GENERATION_FAILED",
            "프로젝트 초대 링크 토큰을 생성하지 못했습니다."
    ),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "일정을 찾을 수 없습니다."),
    AVAILABILITY_REQUEST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AVAILABILITY_REQUEST_NOT_FOUND",
            "일정 조율 요청을 찾을 수 없습니다."
    ),
    AVAILABILITY_RESPONSE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AVAILABILITY_RESPONSE_NOT_FOUND",
            "일정 조율 응답을 찾을 수 없습니다."
    ),
    INVALID_AVAILABILITY_RANGE(
            HttpStatus.BAD_REQUEST,
            "INVALID_AVAILABILITY_RANGE",
            "일정 조율 요청 기간이 올바르지 않습니다."
    ),
    INVALID_SLOT_RANGE(HttpStatus.BAD_REQUEST, "INVALID_SLOT_RANGE", "가능한 시간 범위가 올바르지 않습니다."),
    INVALID_SLOT_UNIT(HttpStatus.BAD_REQUEST, "INVALID_SLOT_UNIT", "가능한 시간 단위가 올바르지 않습니다."),
    INVALID_RECOMMENDED_SLOT(
            HttpStatus.BAD_REQUEST,
            "INVALID_RECOMMENDED_SLOT",
            "모든 프로젝트 멤버가 가능한 시간이 아닙니다."
    ),
    AVAILABILITY_REQUEST_DUPLICATED(
            HttpStatus.CONFLICT,
            "AVAILABILITY_REQUEST_DUPLICATED",
            "동일한 기간의 일정 조율 요청이 이미 존재합니다."
    ),
    AVAILABILITY_REQUEST_ALREADY_CONFIRMED(
            HttpStatus.CONFLICT,
            "AVAILABILITY_REQUEST_ALREADY_CONFIRMED",
            "이미 확정된 일정 조율 요청입니다."
    ),
    AVAILABILITY_REQUEST_CANCELED(
            HttpStatus.CONFLICT,
            "AVAILABILITY_REQUEST_CANCELED",
            "취소된 일정 조율 요청입니다."
    ),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다."),
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
