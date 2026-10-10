package com.wrap.domain.chat.service;

import com.wrap.domain.chat.dto.request.ChatRoomCreateRequest;
import com.wrap.domain.chat.dto.request.ChatRoomUpdateRequest;
import com.wrap.domain.chat.dto.response.ChatMemberResponse;
import com.wrap.domain.chat.dto.response.ChatRoomDetailResponse;
import com.wrap.domain.chat.dto.response.ChatRoomListResponse;
import com.wrap.domain.chat.dto.response.ChatRoomMutationResponse;
import com.wrap.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.wrap.domain.chat.entity.ChatReadState;
import com.wrap.domain.chat.entity.ChatMessage;
import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.entity.ChatRoomMember;
import com.wrap.domain.chat.enums.ChatRoomStatus;
import com.wrap.domain.chat.exception.ChatErrorCode;
import com.wrap.domain.chat.exception.ChatException;
import com.wrap.domain.chat.repository.ChatReadStateRepository;
import com.wrap.domain.chat.repository.ChatMessageRepository;
import com.wrap.domain.chat.repository.ChatRoomMemberRepository;
import com.wrap.domain.chat.repository.ChatRoomRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ScheduleRepository scheduleRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatReadStateRepository chatReadStateRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatRoomDetailResponse create(
            Long memberId,
            Long projectId,
            ChatRoomCreateRequest request
    ) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.PROJECT_NOT_FOUND));
        ProjectMember creator = findJoinedProjectMember(memberId, projectId);
        Schedule schedule = findSchedule(projectId, request.scheduleId());
        validateScheduleIsAvailable(schedule);
        List<ProjectMember> participants = findParticipants(
                projectId,
                creator,
                request.participantProjectMemberIds()
        );

        ChatRoom chatRoom;
        try {
            chatRoom = chatRoomRepository.saveAndFlush(
                    ChatRoom.create(project, schedule, creator, request.name().trim())
            );
        } catch (DataIntegrityViolationException exception) {
            if (schedule != null) {
                throw new ChatException(ChatErrorCode.CHAT_ROOM_SCHEDULE_ALREADY_LINKED);
            }
            throw exception;
        }

        List<ChatRoomMember> chatRoomMembers = participants.stream()
                .map(projectMember -> ChatRoomMember.create(chatRoom, projectMember))
                .toList();
        chatRoomMemberRepository.saveAll(chatRoomMembers);
        chatReadStateRepository.saveAll(participants.stream()
                .map(projectMember -> ChatReadState.create(chatRoom, projectMember))
                .toList());

        return toDetailResponse(chatRoom, chatRoomMembers);
    }

    public ChatRoomListResponse getChatRooms(
            Long memberId,
            ChatRoomStatus status,
            Long projectId,
            String query,
            Long cursor,
            int size
    ) {
        String normalizedQuery = normalizeQuery(query);
        List<ChatRoom> fetched = chatRoomRepository.findVisibleChatRooms(
                memberId,
                status,
                projectId,
                normalizedQuery,
                cursor,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = fetched.size() > size;
        List<ChatRoom> chatRooms = hasNext ? fetched.subList(0, size) : fetched;
        List<ChatRoomSummaryResponse> content = chatRooms.stream()
                .map(chatRoom -> ChatRoomSummaryResponse.from(
                        chatRoom,
                        chatRoomMemberRepository.countByChatRoomIdAndProjectMemberStatus(
                                chatRoom.getId(),
                                ProjectMemberStatus.JOINED
                        ),
                        calculateUnreadCount(chatRoom, memberId)
                ))
                .toList();
        Long nextCursor = hasNext && !chatRooms.isEmpty()
                ? chatRooms.getLast().getId()
                : null;
        return new ChatRoomListResponse(content, nextCursor, hasNext);
    }

    public ChatRoomDetailResponse getChatRoom(Long memberId, Long chatRoomId) {
        ChatRoom chatRoom = findActiveChatRoom(chatRoomId);
        if (findJoinedChatRoomMember(chatRoomId, memberId).isEmpty()) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_REQUIRED);
        }
        return toDetailResponse(
                chatRoom,
                chatRoomMemberRepository.findByChatRoomId(chatRoomId)
        );
    }

    public List<ChatMemberResponse> getMembers(Long memberId, Long chatRoomId) {
        findActiveChatRoom(chatRoomId);
        if (findJoinedChatRoomMember(chatRoomId, memberId).isEmpty()) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_REQUIRED);
        }
        return chatRoomMemberRepository.findByChatRoomId(chatRoomId).stream()
                .filter(chatRoomMember -> chatRoomMember.getProjectMember().getStatus()
                        == ProjectMemberStatus.JOINED)
                .sorted(Comparator.comparing(ChatRoomMember::getId))
                .map(ChatRoomMember::getProjectMember)
                .map(ChatMemberResponse::from)
                .toList();
    }

    @Transactional
    public ChatRoomMutationResponse updateName(
            Long memberId,
            Long chatRoomId,
            ChatRoomUpdateRequest request
    ) {
        ChatRoom chatRoom = findActiveChatRoomForUpdate(chatRoomId);
        ProjectMember currentProjectMember = findCurrentProjectMember(
                memberId,
                chatRoom,
                ChatErrorCode.CHAT_ROOM_UPDATE_FORBIDDEN
        );
        if (!isCreator(chatRoom, currentProjectMember) && !isOwner(currentProjectMember)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_UPDATE_FORBIDDEN);
        }
        if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_CLOSED);
        }

        chatRoom.updateName(request.name().trim());
        return ChatRoomMutationResponse.from(chatRoom);
    }

    @Transactional
    public ChatRoomMutationResponse close(Long memberId, Long chatRoomId) {
        ChatRoom chatRoom = findActiveChatRoomForUpdate(chatRoomId);
        ProjectMember currentProjectMember = findCurrentProjectMember(
                memberId,
                chatRoom,
                ChatErrorCode.CHAT_ROOM_CLOSE_FORBIDDEN
        );
        boolean participant = chatRoomMemberRepository
                .existsByChatRoomIdAndProjectMemberId(
                        chatRoomId,
                        currentProjectMember.getId()
                );
        if (!participant && !isOwner(currentProjectMember)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_CLOSE_FORBIDDEN);
        }
        if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ALREADY_CLOSED);
        }

        chatRoom.close(currentProjectMember, LocalDateTime.now());
        return ChatRoomMutationResponse.from(chatRoom);
    }

    @Transactional
    public void delete(Long memberId, Long chatRoomId) {
        ChatRoom chatRoom = findActiveChatRoomForUpdate(chatRoomId);
        ProjectMember currentProjectMember = findCurrentProjectMember(
                memberId,
                chatRoom,
                ChatErrorCode.CHAT_ROOM_DELETE_FORBIDDEN
        );
        if (!isCreator(chatRoom, currentProjectMember) && !isOwner(currentProjectMember)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_DELETE_FORBIDDEN);
        }
        chatRoom.softDelete(currentProjectMember, LocalDateTime.now());
    }

    private ProjectMember findJoinedProjectMember(Long memberId, Long projectId) {
        return projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                        memberId,
                        projectId,
                        ProjectMemberStatus.JOINED
                )
                .orElseThrow(() -> new ChatException(ChatErrorCode.INVALID_PROJECT_MEMBER));
    }

    private ProjectMember findCurrentProjectMember(
            Long memberId,
            ChatRoom chatRoom,
            ChatErrorCode forbiddenErrorCode
    ) {
        return projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                        memberId,
                        chatRoom.getProject().getId(),
                        ProjectMemberStatus.JOINED
                )
                .orElseThrow(() -> new ChatException(forbiddenErrorCode));
    }

    private Schedule findSchedule(Long projectId, Long scheduleId) {
        if (scheduleId == null) {
            return null;
        }
        return scheduleRepository.findByIdAndProjectId(scheduleId, projectId)
                .filter(Schedule::isShared)
                .orElseThrow(() -> new ChatException(ChatErrorCode.SCHEDULE_NOT_FOUND));
    }

    private void validateScheduleIsAvailable(Schedule schedule) {
        if (schedule != null && chatRoomRepository.existsByScheduleId(schedule.getId())) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_SCHEDULE_ALREADY_LINKED);
        }
    }

    private List<ProjectMember> findParticipants(
            Long projectId,
            ProjectMember creator,
            List<Long> requestedParticipantIds
    ) {
        Set<Long> participantIds = new LinkedHashSet<>(requestedParticipantIds);
        participantIds.add(creator.getId());
        List<ProjectMember> participants = projectMemberRepository
                .findByProjectIdAndIdInAndStatus(
                        projectId,
                        new ArrayList<>(participantIds),
                        ProjectMemberStatus.JOINED
                );
        if (participants.size() != participantIds.size()) {
            throw new ChatException(ChatErrorCode.INVALID_PROJECT_MEMBER);
        }
        return participants;
    }

    private ChatRoom findActiveChatRoom(Long chatRoomId) {
        return chatRoomRepository.findByIdAndDeletedAtIsNull(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoom findActiveChatRoomForUpdate(Long chatRoomId) {
        return chatRoomRepository.findForUpdate(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private Optional<ChatRoomMember> findJoinedChatRoomMember(
            Long chatRoomId,
            Long memberId
    ) {
        return chatRoomMemberRepository
                .findByChatRoomIdAndProjectMemberMemberIdAndProjectMemberStatus(
                        chatRoomId,
                        memberId,
                        ProjectMemberStatus.JOINED
                );
    }

    private long calculateUnreadCount(ChatRoom chatRoom, Long memberId) {
        Optional<ChatRoomMember> chatRoomMember = findJoinedChatRoomMember(
                chatRoom.getId(),
                memberId
        );
        if (chatRoomMember.isEmpty()) {
            return 0;
        }
        Long projectMemberId = chatRoomMember.get().getProjectMember().getId();
        Long lastReadMessageId = chatReadStateRepository
                .findByChatRoomIdAndProjectMemberId(chatRoom.getId(), projectMemberId)
                .map(ChatReadState::getLastReadMessage)
                .map(ChatMessage::getId)
                .orElse(null);
        return chatMessageRepository.countUnreadMessages(
                chatRoom.getId(),
                projectMemberId,
                lastReadMessageId
        );
    }

    private boolean isCreator(ChatRoom chatRoom, ProjectMember projectMember) {
        return chatRoom.getCreator().getId().equals(projectMember.getId());
    }

    private boolean isOwner(ProjectMember projectMember) {
        return projectMember.getRole() == ProjectMemberRole.OWNER;
    }

    private ChatRoomDetailResponse toDetailResponse(
            ChatRoom chatRoom,
            List<ChatRoomMember> chatRoomMembers
    ) {
        List<ChatMemberResponse> participants = chatRoomMembers.stream()
                .filter(chatRoomMember -> chatRoomMember.getProjectMember().getStatus()
                        == ProjectMemberStatus.JOINED)
                .sorted(Comparator.comparing(ChatRoomMember::getId))
                .map(ChatRoomMember::getProjectMember)
                .map(ChatMemberResponse::from)
                .toList();
        return ChatRoomDetailResponse.from(chatRoom, participants);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
