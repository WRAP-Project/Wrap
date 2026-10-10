package com.wrap.domain.chat.repository;

import com.wrap.domain.chat.entity.ChatReadState;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatReadStateRepository extends JpaRepository<ChatReadState, Long> {

    Optional<ChatReadState> findByChatRoomIdAndProjectMemberId(
            Long chatRoomId,
            Long projectMemberId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT readState
            FROM ChatReadState readState
            WHERE readState.chatRoom.id = :chatRoomId
              AND readState.projectMember.id = :projectMemberId
            """)
    Optional<ChatReadState> findForUpdate(
            @Param("chatRoomId") Long chatRoomId,
            @Param("projectMemberId") Long projectMemberId
    );
}
