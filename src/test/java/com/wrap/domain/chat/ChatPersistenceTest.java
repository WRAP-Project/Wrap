package com.wrap.domain.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wrap.domain.chat.entity.ChatMessage;
import com.wrap.domain.chat.entity.ChatReadState;
import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.entity.ChatRoomMember;
import com.wrap.domain.chat.enums.ChatMessageType;
import com.wrap.domain.chat.enums.ChatRoomStatus;
import com.wrap.domain.chat.repository.ChatMessageRepository;
import com.wrap.domain.chat.repository.ChatReadStateRepository;
import com.wrap.domain.chat.repository.ChatRoomMemberRepository;
import com.wrap.domain.chat.repository.ChatRoomRepository;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.schedule.entity.Schedule;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ChatPersistenceTest {

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

    @Test
    void scheduledChatRoomMustBeUniquePerSchedule() {
        ChatFixture fixture = createFixture();
        Schedule schedule = createSchedule(fixture.project(), fixture.member());

        chatRoomRepository.saveAndFlush(ChatRoom.create(
                fixture.project(), schedule, fixture.projectMember(), "첫 번째 회의"
        ));

        assertThrows(DataIntegrityViolationException.class, () ->
                chatRoomRepository.saveAndFlush(ChatRoom.create(
                        fixture.project(), schedule, fixture.projectMember(), "두 번째 회의"
                ))
        );
    }

    @Test
    void instantChatRoomsCanCoexistWithoutSchedule() {
        ChatFixture fixture = createFixture();

        ChatRoom first = chatRoomRepository.saveAndFlush(ChatRoom.create(
                fixture.project(), null, fixture.projectMember(), "즉석 채팅 1"
        ));
        ChatRoom second = chatRoomRepository.saveAndFlush(ChatRoom.create(
                fixture.project(), null, fixture.projectMember(), "즉석 채팅 2"
        ));

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertFalse(first.getId().equals(second.getId()));
        assertEquals(ChatRoomStatus.OPEN, first.getStatus());
    }

    @Test
    void participantCannotBeAddedToSameChatRoomTwice() {
        ChatFixture fixture = createFixture();
        ChatRoom chatRoom = createChatRoom(fixture);

        chatRoomMemberRepository.saveAndFlush(
                ChatRoomMember.create(chatRoom, fixture.projectMember())
        );

        assertThrows(DataIntegrityViolationException.class, () ->
                chatRoomMemberRepository.saveAndFlush(
                        ChatRoomMember.create(chatRoom, fixture.projectMember())
                )
        );
    }

    @Test
    void readStateMustBeUniquePerParticipantAndChatRoom() {
        ChatFixture fixture = createFixture();
        ChatRoom chatRoom = createChatRoom(fixture);

        chatReadStateRepository.saveAndFlush(
                ChatReadState.create(chatRoom, fixture.projectMember())
        );

        assertThrows(DataIntegrityViolationException.class, () ->
                chatReadStateRepository.saveAndFlush(
                        ChatReadState.create(chatRoom, fixture.projectMember())
                )
        );
    }

    @Test
    void messageAndReadStateKeepTheirChatRelationships() {
        ChatFixture fixture = createFixture();
        ChatRoom chatRoom = createChatRoom(fixture);
        ChatMessage message = chatMessageRepository.saveAndFlush(
                ChatMessage.text(chatRoom, fixture.projectMember(), "첫 메시지")
        );
        LocalDateTime readAt = LocalDateTime.of(2026, 10, 10, 14, 0);
        ChatReadState readState = ChatReadState.create(chatRoom, fixture.projectMember());
        readState.updateLastRead(message, readAt);
        chatReadStateRepository.saveAndFlush(readState);

        entityManager.clear();

        ChatReadState found = chatReadStateRepository
                .findByChatRoomIdAndProjectMemberId(
                        chatRoom.getId(), fixture.projectMember().getId()
                )
                .orElseThrow();
        ChatMessage foundMessage = chatMessageRepository
                .findByIdAndChatRoomId(message.getId(), chatRoom.getId())
                .orElseThrow();

        assertEquals(message.getId(), found.getLastReadMessage().getId());
        assertEquals(readAt, found.getLastReadAt());
        assertEquals(fixture.projectMember().getId(), foundMessage.getSender().getId());
        assertEquals(ChatMessageType.TEXT, foundMessage.getType());
    }

    @Test
    void messageRequiresSender() {
        ChatFixture fixture = createFixture();
        ChatRoom chatRoom = createChatRoom(fixture);

        assertThrows(DataIntegrityViolationException.class, () ->
                chatMessageRepository.saveAndFlush(
                        ChatMessage.text(chatRoom, null, "작성자가 없는 메시지")
                )
        );
    }

    private ChatRoom createChatRoom(ChatFixture fixture) {
        return chatRoomRepository.saveAndFlush(ChatRoom.create(
                fixture.project(), null, fixture.projectMember(), "테스트 채팅"
        ));
    }

    private ChatFixture createFixture() {
        Project project = instantiate(Project.class);
        ReflectionTestUtils.setField(project, "name", "테스트 프로젝트");
        entityManager.persist(project);

        Member member = instantiate(Member.class);
        ReflectionTestUtils.setField(member, "email", "chat-" + System.nanoTime() + "@test.com");
        ReflectionTestUtils.setField(member, "password", "password");
        ReflectionTestUtils.setField(member, "nickname", "채팅 사용자");
        entityManager.persist(member);

        ProjectMember projectMember = instantiate(ProjectMember.class);
        ReflectionTestUtils.setField(projectMember, "project", project);
        ReflectionTestUtils.setField(projectMember, "member", member);
        ReflectionTestUtils.setField(projectMember, "status", ProjectMemberStatus.JOINED);
        entityManager.persist(projectMember);
        entityManager.flush();

        return new ChatFixture(project, member, projectMember);
    }

    private Schedule createSchedule(Project project, Member creator) {
        Schedule schedule = instantiate(Schedule.class);
        ReflectionTestUtils.setField(schedule, "project", project);
        ReflectionTestUtils.setField(schedule, "creator", creator);
        ReflectionTestUtils.setField(schedule, "title", "테스트 일정");
        ReflectionTestUtils.setField(schedule, "startAt", LocalDateTime.of(2026, 10, 10, 14, 0));
        ReflectionTestUtils.setField(schedule, "endAt", LocalDateTime.of(2026, 10, 10, 15, 0));
        entityManager.persist(schedule);
        entityManager.flush();
        return schedule;
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

    private record ChatFixture(
            Project project,
            Member member,
            ProjectMember projectMember
    ) {
    }
}
