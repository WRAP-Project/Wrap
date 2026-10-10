package com.wrap.domain.chat.service;

import com.wrap.domain.chat.dto.request.ChatMessageCreateRequest;
import com.wrap.domain.chat.dto.request.ChatMessageUpdateRequest;
import com.wrap.domain.chat.dto.response.ChatMessageResponse;
import com.wrap.domain.chat.dto.response.ChatMessageListResponse;
import com.wrap.domain.chat.entity.ChatMessage;
import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.entity.ChatRoomMember;
import com.wrap.domain.chat.enums.ChatRoomStatus;
import com.wrap.domain.chat.exception.ChatErrorCode;
import com.wrap.domain.chat.exception.ChatException;
import com.wrap.domain.chat.repository.ChatMessageRepository;
import com.wrap.domain.chat.repository.ChatRoomMemberRepository;
import com.wrap.domain.chat.repository.ChatRoomRepository;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessageResponse send(
            Long memberId,
            Long chatRoomId,
            ChatMessageCreateRequest request
    ) {
        ChatRoom chatRoom = chatRoomRepository.findForUpdate(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatRoomMember sender = chatRoomMemberRepository
                .findByChatRoomIdAndProjectMemberMemberIdAndProjectMemberStatus(
                        chatRoomId,
                        memberId,
                        ProjectMemberStatus.JOINED
                )
                .orElseThrow(() -> new ChatException(
                        ChatErrorCode.CHAT_ROOM_MEMBER_REQUIRED
                ));
        if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_CLOSED);
        }

        ChatMessage message = chatMessageRepository.saveAndFlush(
                ChatMessage.text(
                        chatRoom,
                        sender.getProjectMember(),
                        request.content().trim()
                )
        );
        return ChatMessageResponse.from(message, sender.getProjectMember().getId());
    }

    public ChatMessageListResponse getMessages(
            Long memberId,
            Long chatRoomId,
            Long cursor,
            Long afterMessageId,
            int size
    ) {
        chatRoomRepository.findByIdAndDeletedAtIsNull(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatRoomMember currentChatRoomMember = chatRoomMemberRepository
                .findByChatRoomIdAndProjectMemberMemberIdAndProjectMemberStatus(
                        chatRoomId,
                        memberId,
                        ProjectMemberStatus.JOINED
                )
                .orElseThrow(() -> new ChatException(
                        ChatErrorCode.CHAT_ROOM_MEMBER_REQUIRED
                ));
        if (cursor != null && afterMessageId != null) {
            throw new ChatException(ChatErrorCode.INVALID_MESSAGE_QUERY);
        }

        MessageSlice messageSlice = loadMessages(
                chatRoomId,
                cursor,
                afterMessageId,
                size
        );
        List<ChatMessageResponse> content = messageSlice.messages().stream()
                .map(message -> ChatMessageResponse.from(
                        message,
                        currentChatRoomMember.getProjectMember().getId()
                ))
                .toList();
        return new ChatMessageListResponse(
                content,
                messageSlice.nextCursor(),
                messageSlice.hasNext()
        );
    }

    @Transactional
    public ChatMessageResponse update(
            Long memberId,
            Long chatRoomId,
            Long messageId,
            ChatMessageUpdateRequest request
    ) {
        MessageWriteContext context = findMessageWriteContext(
                memberId,
                chatRoomId,
                messageId
        );
        context.message().updateContent(
                request.content().trim(),
                LocalDateTime.now()
        );
        return ChatMessageResponse.from(
                context.message(),
                context.currentChatRoomMember().getProjectMember().getId()
        );
    }

    @Transactional
    public void delete(Long memberId, Long chatRoomId, Long messageId) {
        MessageWriteContext context = findMessageWriteContext(
                memberId,
                chatRoomId,
                messageId
        );
        context.message().softDelete(LocalDateTime.now());
    }

    private MessageWriteContext findMessageWriteContext(
            Long memberId,
            Long chatRoomId,
            Long messageId
    ) {
        ChatRoom chatRoom = chatRoomRepository.findForUpdate(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatRoomMember currentChatRoomMember = chatRoomMemberRepository
                .findByChatRoomIdAndProjectMemberMemberIdAndProjectMemberStatus(
                        chatRoomId,
                        memberId,
                        ProjectMemberStatus.JOINED
                )
                .orElseThrow(() -> new ChatException(
                        ChatErrorCode.CHAT_ROOM_MEMBER_REQUIRED
                ));
        if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_CLOSED);
        }
        ChatMessage message = chatMessageRepository
                .findByIdAndChatRoomIdAndDeletedAtIsNull(messageId, chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND));
        if (!message.getSender().getId().equals(
                currentChatRoomMember.getProjectMember().getId()
        )) {
            throw new ChatException(ChatErrorCode.MESSAGE_AUTHOR_REQUIRED);
        }
        return new MessageWriteContext(currentChatRoomMember, message);
    }

    private MessageSlice loadMessages(
            Long chatRoomId,
            Long cursor,
            Long afterMessageId,
            int size
    ) {
        PageRequest page = PageRequest.of(0, size + 1);
        if (afterMessageId != null) {
            List<ChatMessage> fetched = chatMessageRepository.findNewMessages(
                    chatRoomId,
                    afterMessageId,
                    page
            );
            boolean hasNext = fetched.size() > size;
            List<ChatMessage> messages = new ArrayList<>(
                    hasNext ? fetched.subList(0, size) : fetched
            );
            Long nextCursor = hasNext && !messages.isEmpty()
                    ? messages.getLast().getId()
                    : null;
            return new MessageSlice(messages, nextCursor, hasNext);
        }

        List<ChatMessage> fetched = cursor == null
                ? chatMessageRepository.findLatestMessages(chatRoomId, page)
                : chatMessageRepository.findOlderMessages(chatRoomId, cursor, page);
        boolean hasNext = fetched.size() > size;
        List<ChatMessage> messages = new ArrayList<>(
                hasNext ? fetched.subList(0, size) : fetched
        );
        Collections.reverse(messages);
        Long nextCursor = hasNext && !messages.isEmpty()
                ? messages.getFirst().getId()
                : null;
        return new MessageSlice(messages, nextCursor, hasNext);
    }

    private record MessageSlice(
            List<ChatMessage> messages,
            Long nextCursor,
            boolean hasNext
    ) {
    }

    private record MessageWriteContext(
            ChatRoomMember currentChatRoomMember,
            ChatMessage message
    ) {
    }
}
