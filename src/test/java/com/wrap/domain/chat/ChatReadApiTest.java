package com.wrap.domain.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wrap.domain.chat.entity.ChatMessage;
import com.wrap.domain.chat.entity.ChatReadState;
import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.entity.ChatRoomMember;
import com.wrap.domain.chat.repository.ChatMessageRepository;
import com.wrap.domain.chat.repository.ChatReadStateRepository;
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
import java.util.List;
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
class ChatReadApiTest {

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

    @Autowired
    private ChatReadStateRepository chatReadStateRepository;

    private ReadContext context;

    @BeforeEach
    void setUp() {
        Project project = createProject();
        Member readerMember = createMember("read-member");
        ProjectMember reader = createProjectMember(project, readerMember);
        Member otherMember = createMember("read-other");
        ProjectMember other = createProjectMember(project, otherMember);
        entityManager.flush();
        ChatRoom chatRoom = createRoom(project, reader, "읽음 테스트");
        chatRoomMemberRepository.saveAllAndFlush(List.of(
                ChatRoomMember.create(chatRoom, reader),
                ChatRoomMember.create(chatRoom, other)
        ));
        context = new ReadContext(
                project,
                readerMember,
                reader,
                otherMember,
                other,
                chatRoom
        );
    }

    @Test
    void updatesLastReadAndAllowsSamePositionInClosedChatRoom() throws Exception {
        ChatMessage message = createMessage(context.chatRoom(), context.other(), "읽을 메시지");

        updateRead(context.readerMember(), context.chatRoom(), message)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value(context.chatRoom().getId()))
                .andExpect(jsonPath("$.data.lastReadMessageId").value(message.getId()))
                .andExpect(jsonPath("$.data.lastReadAt").isNotEmpty());

        context.chatRoom().close(
                context.reader(),
                LocalDateTime.of(2026, 10, 10, 14, 0)
        );
        chatRoomRepository.saveAndFlush(context.chatRoom());

        updateRead(context.readerMember(), context.chatRoom(), message)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastReadMessageId").value(message.getId()));

        ChatReadState readState = chatReadStateRepository
                .findByChatRoomIdAndProjectMemberId(
                        context.chatRoom().getId(),
                        context.reader().getId()
                )
                .orElseThrow();
        assertEquals(message.getId(), readState.getLastReadMessage().getId());
        assertNotNull(readState.getLastReadAt());
    }

    @Test
    void rejectsMessageFromAnotherChatRoom() throws Exception {
        ChatRoom otherRoom = createRoom(context.project(), context.reader(), "다른 채팅방");
        chatRoomMemberRepository.saveAndFlush(
                ChatRoomMember.create(otherRoom, context.reader())
        );
        ChatMessage otherRoomMessage = createMessage(
                otherRoom,
                context.reader(),
                "다른 방 메시지"
        );

        updateRead(context.readerMember(), context.chatRoom(), otherRoomMessage)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MESSAGE_NOT_FOUND"));
    }

    @Test
    void lastReadPositionCannotMoveBackward() throws Exception {
        ChatMessage first = createMessage(context.chatRoom(), context.other(), "첫 번째");
        ChatMessage second = createMessage(context.chatRoom(), context.other(), "두 번째");

        updateRead(context.readerMember(), context.chatRoom(), second)
                .andExpect(status().isOk());

        updateRead(context.readerMember(), context.chatRoom(), first)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("READ_POSITION_CANNOT_MOVE_BACKWARD"));
    }

    @Test
    void unreadCountExcludesOwnAndDeletedMessagesAndFollowsReadPosition() throws Exception {
        ChatMessage firstOther = createMessage(context.chatRoom(), context.other(), "상대 1");
        createMessage(context.chatRoom(), context.reader(), "내 메시지");
        ChatMessage secondOther = createMessage(context.chatRoom(), context.other(), "상대 2");
        ChatMessage deletedOther = createMessage(context.chatRoom(), context.other(), "삭제됨");
        deletedOther.softDelete(LocalDateTime.of(2026, 10, 10, 14, 0));
        chatMessageRepository.saveAndFlush(deletedOther);

        expectUnreadCount(2);

        updateRead(context.readerMember(), context.chatRoom(), firstOther)
                .andExpect(status().isOk());
        expectUnreadCount(1);

        updateRead(context.readerMember(), context.chatRoom(), secondOther)
                .andExpect(status().isOk());
        expectUnreadCount(0);
    }

    @Test
    void nonParticipantCannotUpdateReadPosition() throws Exception {
        Member outsiderMember = createMember("read-outsider");
        createProjectMember(context.project(), outsiderMember);
        entityManager.flush();
        ChatMessage message = createMessage(context.chatRoom(), context.other(), "읽기 불가");

        updateRead(outsiderMember, context.chatRoom(), message)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_MEMBER_REQUIRED"));
    }

    private org.springframework.test.web.servlet.ResultActions updateRead(
            Member member,
            ChatRoom chatRoom,
            ChatMessage message
    ) throws Exception {
        return mockMvc.perform(put("/chat-rooms/{chatRoomId}/read", chatRoom.getId())
                .header(MEMBER_ID_HEADER, member.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lastReadMessageId\":" + message.getId() + "}"));
    }

    private void expectUnreadCount(int expected) throws Exception {
        mockMvc.perform(get("/chat-rooms")
                        .header(MEMBER_ID_HEADER, context.readerMember().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].unreadCount").value(expected));
    }

    private ChatMessage createMessage(
            ChatRoom chatRoom,
            ProjectMember sender,
            String content
    ) {
        return chatMessageRepository.saveAndFlush(
                ChatMessage.text(chatRoom, sender, content)
        );
    }

    private ChatRoom createRoom(
            Project project,
            ProjectMember creator,
            String name
    ) {
        return chatRoomRepository.saveAndFlush(
                ChatRoom.create(project, null, creator, name)
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

    private record ReadContext(
            Project project,
            Member readerMember,
            ProjectMember reader,
            Member otherMember,
            ProjectMember other,
            ChatRoom chatRoom
    ) {
    }
}
