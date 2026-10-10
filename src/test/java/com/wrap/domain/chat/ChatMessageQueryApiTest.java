package com.wrap.domain.chat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatMessageQueryApiTest {

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

    @Test
    void returnsLatestMessagesAndLoadsOlderMessagesWithCursor() throws Exception {
        ChatContext context = createContext("history-member");
        List<ChatMessage> messages = createMessages(context, 5);

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", context.chatRoom().getId())
                        .header(MEMBER_ID_HEADER, context.member().getId())
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].messageId")
                        .value(messages.get(2).getId()))
                .andExpect(jsonPath("$.data.content[1].messageId")
                        .value(messages.get(3).getId()))
                .andExpect(jsonPath("$.data.content[2].messageId")
                        .value(messages.get(4).getId()))
                .andExpect(jsonPath("$.data.nextCursor").value(messages.get(2).getId()))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", context.chatRoom().getId())
                        .header(MEMBER_ID_HEADER, context.member().getId())
                        .param("cursor", messages.get(2).getId().toString())
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].messageId")
                        .value(messages.get(0).getId()))
                .andExpect(jsonPath("$.data.content[1].messageId")
                        .value(messages.get(1).getId()))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void pollsNewMessagesInAscendingOrderAndReturnsEmptyWhenThereAreNone() throws Exception {
        ChatContext context = createContext("polling-member");
        List<ChatMessage> messages = createMessages(context, 5);

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", context.chatRoom().getId())
                        .header(MEMBER_ID_HEADER, context.member().getId())
                        .param("afterMessageId", messages.get(1).getId().toString())
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].messageId")
                        .value(messages.get(2).getId()))
                .andExpect(jsonPath("$.data.content[1].messageId")
                        .value(messages.get(3).getId()))
                .andExpect(jsonPath("$.data.nextCursor").value(messages.get(3).getId()))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", context.chatRoom().getId())
                        .header(MEMBER_ID_HEADER, context.member().getId())
                        .param("afterMessageId", messages.get(3).getId().toString())
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].messageId")
                        .value(messages.get(4).getId()))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", context.chatRoom().getId())
                        .header(MEMBER_ID_HEADER, context.member().getId())
                        .param("afterMessageId", messages.get(4).getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void rejectsCursorAndAfterMessageIdUsedTogether() throws Exception {
        ChatContext context = createContext("invalid-query-member");
        ChatMessage message = createMessages(context, 1).getFirst();

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", context.chatRoom().getId())
                        .header(MEMBER_ID_HEADER, context.member().getId())
                        .param("cursor", message.getId().toString())
                        .param("afterMessageId", message.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MESSAGE_QUERY"));
    }

    @Test
    void closedRoomMessagesRemainReadableAndDeletedContentIsHidden() throws Exception {
        ChatContext context = createContext("closed-query-member");
        List<ChatMessage> messages = createMessages(context, 2);
        messages.get(1).softDelete(LocalDateTime.of(2026, 10, 10, 14, 0));
        chatMessageRepository.saveAndFlush(messages.get(1));
        context.chatRoom().close(
                context.projectMember(),
                LocalDateTime.of(2026, 10, 10, 14, 1)
        );
        chatRoomRepository.saveAndFlush(context.chatRoom());

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", context.chatRoom().getId())
                        .header(MEMBER_ID_HEADER, context.member().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[1].deleted").value(true))
                .andExpect(jsonPath("$.data.content[1].content").doesNotExist());
    }

    @Test
    void nonParticipantOwnerCannotReadMessages() throws Exception {
        ChatContext context = createContext("message-room-member");
        createMessages(context, 1);
        Member ownerMember = createMember("message-owner");
        createProjectMember(
                context.project(),
                ownerMember,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        );
        flush();

        mockMvc.perform(get("/chat-rooms/{chatRoomId}/messages", context.chatRoom().getId())
                        .header(MEMBER_ID_HEADER, ownerMember.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_MEMBER_REQUIRED"));
    }

    private ChatContext createContext(String uniqueName) {
        Project project = createProject();
        Member member = createMember(uniqueName);
        ProjectMember projectMember = createProjectMember(
                project,
                member,
                ProjectMemberRole.MEMBER,
                ProjectMemberStatus.JOINED
        );
        flush();
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                ChatRoom.create(project, null, projectMember, "메시지 조회 테스트")
        );
        chatRoomMemberRepository.saveAndFlush(
                ChatRoomMember.create(chatRoom, projectMember)
        );
        return new ChatContext(project, member, projectMember, chatRoom);
    }

    private List<ChatMessage> createMessages(ChatContext context, int count) {
        List<ChatMessage> messages = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            messages.add(ChatMessage.text(
                    context.chatRoom(),
                    context.projectMember(),
                    "메시지 " + index
            ));
        }
        return chatMessageRepository.saveAllAndFlush(messages);
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

    private record ChatContext(
            Project project,
            Member member,
            ProjectMember projectMember,
            ChatRoom chatRoom
    ) {
    }
}
