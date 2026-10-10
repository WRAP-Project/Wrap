package com.wrap.domain.chat;

import static com.wrap.domain.chat.ChatTestSecurity.authenticatedAs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wrap.domain.chat.entity.ChatMessage;
import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.entity.ChatRoomMember;
import com.wrap.domain.chat.repository.ChatMessageRepository;
import com.wrap.domain.chat.repository.ChatRoomMemberRepository;
import com.wrap.domain.chat.repository.ChatRoomRepository;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
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
class ChatParticipantAndMessageApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    void participantCanListJoinedMembersAndLeftMemberIsExcluded() throws Exception {
        Project project = createProject();
        Member creatorMember = createMember("members-creator");
        ProjectMember creator = createProjectMember(
                project,
                creatorMember,
                ProjectMemberRole.MEMBER,
                ProjectMemberStatus.JOINED
        );
        ProjectMember joined = createProjectMember(
                project,
                createMember("members-joined"),
                ProjectMemberRole.MEMBER,
                ProjectMemberStatus.JOINED
        );
        ProjectMember left = createProjectMember(
                project,
                createMember("members-left"),
                ProjectMemberRole.MEMBER,
                ProjectMemberStatus.LEFT
        );
        flush();
        ChatRoom chatRoom = createRoom(project, creator);
        addParticipant(chatRoom, creator);
        addParticipant(chatRoom, joined);
        addParticipant(chatRoom, left);

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/members", chatRoom.getId())
                        .with(authenticatedAs(creatorMember.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].projectMemberId").value(creator.getId()))
                .andExpect(jsonPath("$.data[1].projectMemberId").value(joined.getId()));
    }

    @Test
    void nonParticipantOwnerCannotListChatRoomMembers() throws Exception {
        Project project = createProject();
        ProjectMember creator = createProjectMember(
                project,
                createMember("member-list-creator"),
                ProjectMemberRole.MEMBER,
                ProjectMemberStatus.JOINED
        );
        Member ownerMember = createMember("member-list-owner");
        createProjectMember(
                project,
                ownerMember,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        );
        flush();
        ChatRoom chatRoom = createRoom(project, creator);
        addParticipant(chatRoom, creator);

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/members", chatRoom.getId())
                        .with(authenticatedAs(ownerMember.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_MEMBER_REQUIRED"));
    }

    @Test
    void participantCanSendTextMessageToOpenChatRoom() throws Exception {
        Project project = createProject();
        Member senderMember = createMember("message-sender");
        ProjectMember sender = createProjectMember(
                project,
                senderMember,
                ProjectMemberRole.MEMBER,
                ProjectMemberStatus.JOINED
        );
        flush();
        ChatRoom chatRoom = createRoom(project, sender);
        addParticipant(chatRoom, sender);

        mockMvc.perform(post("/chat-rooms/{chatRoomId}/messages", chatRoom.getId())
                        .with(authenticatedAs(senderMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  첫 메시지  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chatRoomId").value(chatRoom.getId()))
                .andExpect(jsonPath("$.data.type").value("TEXT"))
                .andExpect(jsonPath("$.data.sender.projectMemberId").value(sender.getId()))
                .andExpect(jsonPath("$.data.content").value("첫 메시지"))
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").value(false))
                .andExpect(jsonPath("$.data.mine").value(true));

        ChatMessage saved = chatMessageRepository.findAll().getFirst();
        assertEquals("첫 메시지", saved.getContent());
        assertNull(saved.getUpdatedAt());
    }

    @Test
    void nonParticipantCannotSendMessage() throws Exception {
        Project project = createProject();
        ProjectMember creator = createProjectMember(
                project,
                createMember("non-participant-creator"),
                ProjectMemberRole.MEMBER,
                ProjectMemberStatus.JOINED
        );
        Member outsiderMember = createMember("message-outsider");
        createProjectMember(
                project,
                outsiderMember,
                ProjectMemberRole.MEMBER,
                ProjectMemberStatus.JOINED
        );
        flush();
        ChatRoom chatRoom = createRoom(project, creator);
        addParticipant(chatRoom, creator);

        mockMvc.perform(post("/chat-rooms/{chatRoomId}/messages", chatRoom.getId())
                        .with(authenticatedAs(outsiderMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"전송 불가\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_MEMBER_REQUIRED"));
    }

    @Test
    void closedChatRoomRejectsMessageAndBlankContentIsInvalid() throws Exception {
        Project project = createProject();
        Member senderMember = createMember("closed-message-sender");
        ProjectMember sender = createProjectMember(
                project,
                senderMember,
                ProjectMemberRole.MEMBER,
                ProjectMemberStatus.JOINED
        );
        flush();
        ChatRoom chatRoom = createRoom(project, sender);
        addParticipant(chatRoom, sender);

        mockMvc.perform(post("/chat-rooms/{chatRoomId}/messages", chatRoom.getId())
                        .with(authenticatedAs(senderMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        chatRoom.close(sender, LocalDateTime.of(2026, 10, 10, 14, 0));
        chatRoomRepository.saveAndFlush(chatRoom);

        mockMvc.perform(post("/chat-rooms/{chatRoomId}/messages", chatRoom.getId())
                        .with(authenticatedAs(senderMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"종료 후 메시지\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CHAT_ROOM_CLOSED"));
    }

    private ChatRoom createRoom(Project project, ProjectMember creator) {
        return chatRoomRepository.saveAndFlush(
                ChatRoom.create(project, null, creator, "테스트 채팅")
        );
    }

    private void addParticipant(ChatRoom chatRoom, ProjectMember projectMember) {
        chatRoomMemberRepository.saveAndFlush(
                ChatRoomMember.create(chatRoom, projectMember)
        );
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
            ProjectMemberRole role,
            ProjectMemberStatus status
    ) {
        ProjectMember projectMember = instantiate(ProjectMember.class);
        ReflectionTestUtils.setField(projectMember, "project", project);
        ReflectionTestUtils.setField(projectMember, "member", member);
        ReflectionTestUtils.setField(projectMember, "role", role);
        ReflectionTestUtils.setField(projectMember, "status", status);
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
}
