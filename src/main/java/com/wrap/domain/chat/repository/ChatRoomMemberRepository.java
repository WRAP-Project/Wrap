package com.wrap.domain.chat.repository;

import com.wrap.domain.chat.entity.ChatRoomMember;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    Optional<ChatRoomMember> findByChatRoomIdAndProjectMemberId(
            Long chatRoomId,
            Long projectMemberId
    );

    boolean existsByChatRoomIdAndProjectMemberId(Long chatRoomId, Long projectMemberId);

    boolean existsByChatRoomIdAndProjectMemberMemberId(Long chatRoomId, Long memberId);

    Optional<ChatRoomMember> findByChatRoomIdAndProjectMemberMemberIdAndProjectMemberStatus(
            Long chatRoomId,
            Long memberId,
            ProjectMemberStatus status
    );

    long countByChatRoomIdAndProjectMemberStatus(
            Long chatRoomId,
            ProjectMemberStatus status
    );
}
