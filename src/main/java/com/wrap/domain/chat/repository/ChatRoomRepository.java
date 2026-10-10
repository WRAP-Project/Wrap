package com.wrap.domain.chat.repository;

import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.enums.ChatRoomStatus;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT chatRoom
            FROM ChatRoom chatRoom
            WHERE chatRoom.id = :chatRoomId
              AND chatRoom.deletedAt IS NULL
            """)
    Optional<ChatRoom> findForUpdate(@Param("chatRoomId") Long chatRoomId);

    boolean existsByScheduleId(Long scheduleId);

    @Query("""
            SELECT chatRoomMember.chatRoom
            FROM ChatRoomMember chatRoomMember
            WHERE chatRoomMember.projectMember.member.id = :memberId
              AND chatRoomMember.chatRoom.deletedAt IS NULL
              AND chatRoomMember.chatRoom.status = :status
              AND chatRoomMember.projectMember.status =
                  com.wrap.domain.projectmember.enums.ProjectMemberStatus.JOINED
              AND (:projectId IS NULL OR chatRoomMember.chatRoom.project.id = :projectId)
              AND (:query IS NULL OR LOWER(chatRoomMember.chatRoom.name)
                   LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:cursor IS NULL OR chatRoomMember.chatRoom.id < :cursor)
            ORDER BY chatRoomMember.chatRoom.id DESC
            """)
    List<ChatRoom> findVisibleChatRooms(
            @Param("memberId") Long memberId,
            @Param("status") ChatRoomStatus status,
            @Param("projectId") Long projectId,
            @Param("query") String query,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
