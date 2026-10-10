package com.wrap.domain.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.junit.jupiter.api.BeforeEach;
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
class ChatMessageCommandApiTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

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

    private MessageContext context;

    @BeforeEach
    void setUp() {
        Project project = createProject();
        Member authorMember = createMember("message-author");
        ProjectMember author = createProjectMember(project, authorMember);
        Member otherMember = createMember("other-participant");
        ProjectMember otherParticipant = createProjectMember(project, otherMember);
        entityManager.flush();
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                ChatRoom.create(project, null, author, "메시지 명령 테스트")
        );
        chatRoomMemberRepository.saveAllAndFlush(java.util.List.of(
                ChatRoomMember.create(chatRoom, author),
                ChatRoomMember.create(chatRoom, otherParticipant)
        ));
        ChatMessage message = chatMessageRepository.saveAndFlush(
                ChatMessage.text(chatRoom, author, "수정 전 메시지")
        );
        context = new MessageContext(
                authorMember,
                author,
                otherMember,
                chatRoom,
                message
        );
    }

    @Test
    void authorCanUpdateOwnMessage() throws Exception {
        mockMvc.perform(patch(
                        "/chat-rooms/{chatRoomId}/messages/{messageId}",
                        context.chatRoom().getId(),
                        context.message().getId()
                )
                        .header(MEMBER_ID_HEADER, context.authorMember().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  수정된 메시지  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("수정된 메시지"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.deleted").value(false))
                .andExpect(jsonPath("$.data.mine").value(true));

        ChatMessage updated = chatMessageRepository
                .findById(context.message().getId())
                .orElseThrow();
        assertEquals("수정된 메시지", updated.getContent());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void otherParticipantCannotUpdateOrDeleteMessage() throws Exception {
        mockMvc.perform(patch(
                        "/chat-rooms/{chatRoomId}/messages/{messageId}",
                        context.chatRoom().getId(),
                        context.message().getId()
                )
                        .header(MEMBER_ID_HEADER, context.otherMember().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"다른 사람이 수정\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MESSAGE_AUTHOR_REQUIRED"));

        mockMvc.perform(delete(
                        "/chat-rooms/{chatRoomId}/messages/{messageId}",
                        context.chatRoom().getId(),
                        context.message().getId()
                )
                        .header(MEMBER_ID_HEADER, context.otherMember().getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MESSAGE_AUTHOR_REQUIRED"));
    }

    @Test
    void authorSoftDeletesMessageAndCannotDeleteOrUpdateItAgain() throws Exception {
        mockMvc.perform(delete(
                        "/chat-rooms/{chatRoomId}/messages/{messageId}",
                        context.chatRoom().getId(),
                        context.message().getId()
                )
                        .header(MEMBER_ID_HEADER, context.authorMember().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ChatMessage deleted = chatMessageRepository
                .findById(context.message().getId())
                .orElseThrow();
        assertNotNull(deleted.getDeletedAt());

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", context.chatRoom().getId())
                        .header(MEMBER_ID_HEADER, context.authorMember().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].deleted").value(true))
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist());

        mockMvc.perform(delete(
                        "/chat-rooms/{chatRoomId}/messages/{messageId}",
                        context.chatRoom().getId(),
                        context.message().getId()
                )
                        .header(MEMBER_ID_HEADER, context.authorMember().getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MESSAGE_NOT_FOUND"));

        mockMvc.perform(patch(
                        "/chat-rooms/{chatRoomId}/messages/{messageId}",
                        context.chatRoom().getId(),
                        context.message().getId()
                )
                        .header(MEMBER_ID_HEADER, context.authorMember().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"삭제 후 수정\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MESSAGE_NOT_FOUND"));
    }

    @Test
    void closedChatRoomRejectsMessageUpdateAndDelete() throws Exception {
        context.chatRoom().close(
                context.author(),
                LocalDateTime.of(2026, 10, 10, 14, 0)
        );
        chatRoomRepository.saveAndFlush(context.chatRoom());

        mockMvc.perform(patch(
                        "/chat-rooms/{chatRoomId}/messages/{messageId}",
                        context.chatRoom().getId(),
                        context.message().getId()
                )
                        .header(MEMBER_ID_HEADER, context.authorMember().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"종료 후 수정\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_CLOSED"));

        mockMvc.perform(delete(
                        "/chat-rooms/{chatRoomId}/messages/{messageId}",
                        context.chatRoom().getId(),
                        context.message().getId()
                )
                        .header(MEMBER_ID_HEADER, context.authorMember().getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_CLOSED"));
    }

    @Test
    void blankMessageUpdateIsInvalid() throws Exception {
        mockMvc.perform(patch(
                        "/chat-rooms/{chatRoomId}/messages/{messageId}",
                        context.chatRoom().getId(),
                        context.message().getId()
                )
                        .header(MEMBER_ID_HEADER, context.authorMember().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
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

    private ProjectMember createProjectMember(Project project, Member member) {
        ProjectMember projectMember = instantiate(ProjectMember.class);
        ReflectionTestUtils.setField(projectMember, "project", project);
        ReflectionTestUtils.setField(projectMember, "member", member);
        ReflectionTestUtils.setField(projectMember, "role", ProjectMemberRole.MEMBER);
        ReflectionTestUtils.setField(projectMember, "status", ProjectMemberStatus.JOINED);
        entityManager.persist(projectMember);
        return projectMember;
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

    private record MessageContext(
            Member authorMember,
            ProjectMember author,
            Member otherMember,
            ChatRoom chatRoom,
            ChatMessage message
    ) {
    }
}
