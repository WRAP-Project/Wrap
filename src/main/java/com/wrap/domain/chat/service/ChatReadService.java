package com.wrap.domain.chat.service;

import com.wrap.domain.chat.dto.request.ChatReadRequest;
import com.wrap.domain.chat.dto.response.ChatReadResponse;
import com.wrap.domain.chat.entity.ChatMessage;
import com.wrap.domain.chat.entity.ChatReadState;
import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.entity.ChatRoomMember;
import com.wrap.domain.chat.exception.ChatErrorCode;
import com.wrap.domain.chat.exception.ChatException;
import com.wrap.domain.chat.repository.ChatMessageRepository;
import com.wrap.domain.chat.repository.ChatReadStateRepository;
import com.wrap.domain.chat.repository.ChatRoomMemberRepository;
import com.wrap.domain.chat.repository.ChatRoomRepository;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReadService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReadStateRepository chatReadStateRepository;

    @Transactional
    public ChatReadResponse updateLastRead(
            Long memberId,
            Long chatRoomId,
            ChatReadRequest request
    ) {
        ChatRoom chatRoom = chatRoomRepository.findByIdAndDeletedAtIsNull(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatRoomMember chatRoomMember = chatRoomMemberRepository
                .findByChatRoomIdAndProjectMemberMemberIdAndProjectMemberStatus(
                        chatRoomId,
                        memberId,
                        ProjectMemberStatus.JOINED
                )
                .orElseThrow(() -> new ChatException(
                        ChatErrorCode.CHAT_ROOM_MEMBER_REQUIRED
                ));
        ChatMessage lastReadMessage = chatMessageRepository
                .findByIdAndChatRoomId(request.lastReadMessageId(), chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND));
        Long projectMemberId = chatRoomMember.getProjectMember().getId();
        ChatReadState readState = chatReadStateRepository
                .findForUpdate(chatRoomId, projectMemberId)
                .orElseGet(() -> ChatReadState.create(
                        chatRoom,
                        chatRoomMember.getProjectMember()
                ));
        if (readState.getLastReadMessage() != null
                && readState.getLastReadMessage().getId() > lastReadMessage.getId()) {
            throw new ChatException(ChatErrorCode.READ_POSITION_CANNOT_MOVE_BACKWARD);
        }

        readState.updateLastRead(lastReadMessage, LocalDateTime.now());
        ChatReadState saved = chatReadStateRepository.saveAndFlush(readState);
        return ChatReadResponse.from(saved);
    }
}
