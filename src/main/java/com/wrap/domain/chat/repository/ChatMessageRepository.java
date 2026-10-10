package com.wrap.domain.chat.repository;

import com.wrap.domain.chat.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByIdAndChatRoomId(Long id, Long chatRoomId);

    Optional<ChatMessage> findByIdAndChatRoomIdAndDeletedAtIsNull(
            Long id,
            Long chatRoomId
    );

    @Query("""
            SELECT message
            FROM ChatMessage message
            WHERE message.chatRoom.id = :chatRoomId
              AND message.id < :cursor
            ORDER BY message.id DESC
            """)
    List<ChatMessage> findOlderMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            SELECT message
            FROM ChatMessage message
            WHERE message.chatRoom.id = :chatRoomId
              AND message.id > :afterMessageId
            ORDER BY message.id ASC
            """)
    List<ChatMessage> findNewMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("afterMessageId") Long afterMessageId,
            Pageable pageable
    );

    @Query("""
            SELECT message
            FROM ChatMessage message
            WHERE message.chatRoom.id = :chatRoomId
            ORDER BY message.id DESC
            """)
    List<ChatMessage> findLatestMessages(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(message)
            FROM ChatMessage message
            WHERE message.chatRoom.id = :chatRoomId
              AND message.deletedAt IS NULL
              AND message.sender.id <> :projectMemberId
              AND (:lastReadMessageId IS NULL OR message.id > :lastReadMessageId)
            """)
    long countUnreadMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("projectMemberId") Long projectMemberId,
            @Param("lastReadMessageId") Long lastReadMessageId
    );
}
