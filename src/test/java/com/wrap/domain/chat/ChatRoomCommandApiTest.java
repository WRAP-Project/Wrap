package com.wrap.domain.chat;

import static com.wrap.domain.chat.ChatTestSecurity.authenticatedAs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.entity.ChatRoomMember;
import com.wrap.domain.chat.enums.ChatRoomStatus;
import com.wrap.domain.chat.repository.ChatRoomMemberRepository;
import com.wrap.domain.chat.repository.ChatRoomRepository;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatRoomCommandApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Test
    void creatorCanUpdateChatRoomName() throws Exception {
        ChatContext context = createContext("update-creator", ProjectMemberRole.MEMBER);
        ChatRoom chatRoom = createRoom(context, "수정 전", true);

        mockMvc.perform(patch("/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .with(authenticatedAs(context.member().getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"수정 후\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chatRoomId").value(chatRoom.getId()))
                .andExpect(jsonPath("$.data.name").value("수정 후"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.participants").doesNotExist());

        assertEquals("수정 후", chatRoomRepository.findById(chatRoom.getId()).orElseThrow().getName());
    }

    @Test
    void onlyCreatorOrOwnerCanUpdateChatRoomName() throws Exception {
        Project project = createProject();
        Member creatorMember = createMember("name-creator");
        ProjectMember creator = createProjectMember(
                project,
                creatorMember,
                ProjectMemberRole.MEMBER
        );
        Member participantMember = createMember("name-participant");
        ProjectMember participant = createProjectMember(
                project,
                participantMember,
                ProjectMemberRole.MEMBER
        );
        Member ownerMember = createMember("name-owner");
        createProjectMember(project, ownerMember, ProjectMemberRole.OWNER);
        flush();
        ChatRoom chatRoom = createRoom(project, creator, "수정 권한 테스트", true);
        chatRoomMemberRepository.saveAndFlush(ChatRoomMember.create(chatRoom, participant));

        mockMvc.perform(patch("/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .with(authenticatedAs(participantMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"참여자 수정\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_UPDATE_FORBIDDEN"));

        mockMvc.perform(patch("/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .with(authenticatedAs(ownerMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"OWNER 수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("OWNER 수정"));
    }

    @Test
    void nonParticipantOwnerCanCloseWithoutReceivingDetailData() throws Exception {
        Project project = createProject();
        Member creatorMember = createMember("close-creator");
        ProjectMember creator = createProjectMember(
                project,
                creatorMember,
                ProjectMemberRole.MEMBER
        );
        Member ownerMember = createMember("close-owner");
        ProjectMember owner = createProjectMember(
                project,
                ownerMember,
                ProjectMemberRole.OWNER
        );
        flush();
        ChatRoom chatRoom = createRoom(project, creator, "종료 테스트", true);

        mockMvc.perform(patch("/chat-rooms/{chatRoomId}/close", chatRoom.getId())
                        .with(authenticatedAs(ownerMember.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.participants").doesNotExist());

        ChatRoom closed = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
        assertEquals(ChatRoomStatus.CLOSED, closed.getStatus());
        assertEquals(owner.getId(), closed.getClosedBy().getId());
        assertNotNull(closed.getClosedAt());

        mockMvc.perform(patch("/chat-rooms/{chatRoomId}/close", chatRoom.getId())
                        .with(authenticatedAs(ownerMember.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_ALREADY_CLOSED"));

        mockMvc.perform(patch("/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .with(authenticatedAs(creatorMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"종료 후 수정\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_CLOSED"));
    }

    @Test
    void regularNonParticipantCannotCloseChatRoom() throws Exception {
        Project project = createProject();
        Member creatorMember = createMember("close-member-creator");
        ProjectMember creator = createProjectMember(
                project,
                creatorMember,
                ProjectMemberRole.MEMBER
        );
        Member outsiderMember = createMember("close-outsider");
        createProjectMember(project, outsiderMember, ProjectMemberRole.MEMBER);
        flush();
        ChatRoom chatRoom = createRoom(project, creator, "종료 권한 테스트", true);

        mockMvc.perform(patch("/chat-rooms/{chatRoomId}/close", chatRoom.getId())
                        .with(authenticatedAs(outsiderMember.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_CLOSE_FORBIDDEN"));
    }

    @Test
    void participantCanCloseChatRoom() throws Exception {
        Project project = createProject();
        Member creatorMember = createMember("participant-close-creator");
        ProjectMember creator = createProjectMember(
                project,
                creatorMember,
                ProjectMemberRole.MEMBER
        );
        Member participantMember = createMember("participant-close-member");
        ProjectMember participant = createProjectMember(
                project,
                participantMember,
                ProjectMemberRole.MEMBER
        );
        flush();
        ChatRoom chatRoom = createRoom(project, creator, "참여자 종료 테스트", true);
        chatRoomMemberRepository.saveAndFlush(ChatRoomMember.create(chatRoom, participant));

        mockMvc.perform(patch("/chat-rooms/{chatRoomId}/close", chatRoom.getId())
                        .with(authenticatedAs(participantMember.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        assertEquals(
                participant.getId(),
                chatRoomRepository.findById(chatRoom.getId()).orElseThrow().getClosedBy().getId()
        );
    }

    @Test
    void creatorCanSoftDeleteChatRoomAndGeneralQueriesExcludeIt() throws Exception {
        ChatContext context = createContext("delete-creator", ProjectMemberRole.MEMBER);
        ChatRoom chatRoom = createRoom(context, "삭제 테스트", true);

        mockMvc.perform(delete("/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .with(authenticatedAs(context.member().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        ChatRoom deleted = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
        assertNotNull(deleted.getDeletedAt());
        assertEquals(context.projectMember().getId(), deleted.getDeletedBy().getId());
        assertTrue(chatRoomRepository.findByIdAndDeletedAtIsNull(chatRoom.getId()).isEmpty());

        mockMvc.perform(get("/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .with(authenticatedAs(context.member().getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    void participantWhoIsNotCreatorCannotDeleteChatRoom() throws Exception {
        Project project = createProject();
        Member creatorMember = createMember("delete-room-creator");
        ProjectMember creator = createProjectMember(
                project,
                creatorMember,
                ProjectMemberRole.MEMBER
        );
        Member participantMember = createMember("delete-participant");
        ProjectMember participant = createProjectMember(
                project,
                participantMember,
                ProjectMemberRole.MEMBER
        );
        flush();
        ChatRoom chatRoom = createRoom(project, creator, "삭제 권한 테스트", true);
        chatRoomMemberRepository.saveAndFlush(ChatRoomMember.create(chatRoom, participant));

        mockMvc.perform(delete("/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .with(authenticatedAs(participantMember.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_DELETE_FORBIDDEN"));
    }

    private ChatContext createContext(String uniqueName, ProjectMemberRole role) {
        Project project = createProject();
        Member member = createMember(uniqueName);
        ProjectMember projectMember = createProjectMember(project, member, role);
        flush();
        return new ChatContext(project, member, projectMember);
    }

    private ChatRoom createRoom(ChatContext context, String name, boolean addCreator) {
        return createRoom(context.project(), context.projectMember(), name, addCreator);
    }

    private ChatRoom createRoom(
            Project project,
            ProjectMember creator,
            String name,
            boolean addCreator
    ) {
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                ChatRoom.create(project, null, creator, name)
        );
        if (addCreator) {
            chatRoomMemberRepository.saveAndFlush(ChatRoomMember.create(chatRoom, creator));
        }
        return chatRoom;
    }

    private Project createProject() {
        Project project = instantiate(Project.class);
        ReflectionTestUtils.setField(project, "name", "프로젝트 루프");
        entityManager.persist(project);
        return project;
    }

    private Member createMember(String uniqueName) {
        Member member = instantiate(Member.class);
        ReflectionTestUtils.setField(member, "email", uniqueName + "@test.com");
        ReflectionTestUtils.setField(member, "password", "password");
        ReflectionTestUtils.setField(member, "nickname", uniqueName);
        entityManager.persist(member);
        return member;
    }

    private ProjectMember createProjectMember(
            Project project,
            Member member,
            ProjectMemberRole role
    ) {
        ProjectMember projectMember = instantiate(ProjectMember.class);
        ReflectionTestUtils.setField(projectMember, "project", project);
        ReflectionTestUtils.setField(projectMember, "member", member);
        ReflectionTestUtils.setField(projectMember, "role", role);
        ReflectionTestUtils.setField(projectMember, "status", ProjectMemberStatus.JOINED);
        entityManager.persist(projectMember);
        return projectMember;
    }

    private void flush() {
        entityManager.flush();
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("테스트 엔티티를 생성할 수 없습니다.", exception);
        }
    }

    private record ChatContext(
            Project project,
            Member member,
            ProjectMember projectMember
    ) {
    }
}
