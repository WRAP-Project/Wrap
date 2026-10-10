package com.wrap.domain.chat.exception;

import org.springframework.http.HttpStatus;

public enum ChatErrorCode {
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    INVALID_PROJECT_MEMBER(HttpStatus.FORBIDDEN, "참여할 수 없는 프로젝트 멤버입니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트에 속한 일정을 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_MEMBER_REQUIRED(HttpStatus.FORBIDDEN, "채팅방 참여자만 조회할 수 있습니다."),
    CHAT_ROOM_CLOSED(HttpStatus.CONFLICT, "종료된 채팅방에서는 변경할 수 없습니다."),
    CHAT_ROOM_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 종료된 채팅방입니다."),
    CHAT_ROOM_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "채팅방 이름 수정 권한이 없습니다."),
    CHAT_ROOM_CLOSE_FORBIDDEN(HttpStatus.FORBIDDEN, "채팅방 종료 권한이 없습니다."),
    CHAT_ROOM_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "채팅방 삭제 권한이 없습니다."),
    INVALID_MESSAGE_QUERY(
            HttpStatus.BAD_REQUEST,
            "cursor와 afterMessageId는 동시에 사용할 수 없습니다."
    ),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    MESSAGE_AUTHOR_REQUIRED(HttpStatus.FORBIDDEN, "메시지 작성자만 변경할 수 있습니다."),
    READ_POSITION_CANNOT_MOVE_BACKWARD(
            HttpStatus.CONFLICT,
            "마지막 읽음 위치를 이전 메시지로 되돌릴 수 없습니다."
    ),
    CHAT_ROOM_SCHEDULE_ALREADY_LINKED(
            HttpStatus.CONFLICT,
            "해당 일정에는 이미 채팅방이 연결되어 있습니다."
    );

    private final HttpStatus status;
    private final String message;

    ChatErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
