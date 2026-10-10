package com.wrap.domain.chat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.entity.ChatRoomMember;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatRoomApiTest {

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
    private ChatReadStateRepository chatReadStateRepository;

    @Test
    void createsInstantChatRoomAndAutomaticallyAddsCreator() throws Exception {
        Project project = createProject("프로젝트 루프");
        Member creatorMember = createMember("creator");
        ProjectMember creator = createProjectMember(project, creatorMember, ProjectMemberStatus.JOINED);
        Member participantMember = createMember("participant");
        ProjectMember participant = createProjectMember(
                project,
                participantMember,
                ProjectMemberStatus.JOINED
        );
        flush();

        String requestBody = """
                {
                  "name": "긴급 디자인 논의",
                  "participantProjectMemberIds": [%d],
                  "scheduleId": null
                }
                """.formatted(participant.getId());

        mockMvc.perform(post("/projects/{projectId}/chat-rooms", project.getId())
                        .header(MEMBER_ID_HEADER, creatorMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("긴급 디자인 논의"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.schedule").doesNotExist())
                .andExpect(jsonPath("$.data.creator.projectMemberId").value(creator.getId()))
                .andExpect(jsonPath("$.data.participants.length()").value(2));

        ChatRoom chatRoom = chatRoomRepository.findAll().getFirst();
        org.junit.jupiter.api.Assertions.assertTrue(
                chatRoomMemberRepository.existsByChatRoomIdAndProjectMemberId(
                        chatRoom.getId(),
                        creator.getId()
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(2, chatReadStateRepository.count());
    }

    @Test
    void rejectsSecondChatRoomForSameSchedule() throws Exception {
        Project project = createProject("프로젝트 루프");
        Member creatorMember = createMember("schedule-owner");
        createProjectMember(project, creatorMember, ProjectMemberStatus.JOINED);
        Schedule schedule = createSchedule(project, creatorMember);
        flush();
        String requestBody = """
                {
                  "name": "예약 회의",
                  "participantProjectMemberIds": [],
                  "scheduleId": %d
                }
                """.formatted(schedule.getId());

        mockMvc.perform(post("/projects/{projectId}/chat-rooms", project.getId())
                        .header(MEMBER_ID_HEADER, creatorMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/projects/{projectId}/chat-rooms", project.getId())
                        .header(MEMBER_ID_HEADER, creatorMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("CHAT_ROOM_SCHEDULE_ALREADY_LINKED"));
    }

    @Test
    void rejectsScheduleThatIsNotShared() throws Exception {
        Project project = createProject("프로젝트 루프");
        Member creatorMember = createMember("private-schedule-owner");
        createProjectMember(project, creatorMember, ProjectMemberStatus.JOINED);
        Schedule schedule = createSchedule(project, creatorMember);
        ReflectionTestUtils.setField(schedule, "shared", false);
        flush();
        String requestBody = """
                {
                  "name": "개인 일정 채팅",
                  "participantProjectMemberIds": [],
                  "scheduleId": %d
                }
                """.formatted(schedule.getId());

        mockMvc.perform(post("/projects/{projectId}/chat-rooms", project.getId())
                        .header(MEMBER_ID_HEADER, creatorMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void rejectsParticipantWhoIsNotJoinedProjectMember() throws Exception {
        Project project = createProject("프로젝트 루프");
        Member creatorMember = createMember("joined-creator");
        createProjectMember(project, creatorMember, ProjectMemberStatus.JOINED);
        Member invitedMember = createMember("invited-participant");
        ProjectMember invitedProjectMember = createProjectMember(
                project,
                invitedMember,
                ProjectMemberStatus.INVITED
        );
        flush();
        String requestBody = """
                {
                  "name": "참여자 검증 채팅",
                  "participantProjectMemberIds": [%d],
                  "scheduleId": null
                }
                """.formatted(invitedProjectMember.getId());

        mockMvc.perform(post("/projects/{projectId}/chat-rooms", project.getId())
                        .header(MEMBER_ID_HEADER, creatorMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_PROJECT_MEMBER"));
    }

    @Test
    void listsParticipatingChatRoomsWithStatusProjectAndNameFilters() throws Exception {
        Project project = createProject("프로젝트 루프");
        Project otherProject = createProject("다른 프로젝트");
        Member member = createMember("filter-member");
        ProjectMember projectMember = createProjectMember(
                project,
                member,
                ProjectMemberStatus.JOINED
        );
        ProjectMember otherProjectMember = createProjectMember(
                otherProject,
                member,
                ProjectMemberStatus.JOINED
        );
        flush();
        ChatRoom openRoom = createRoom(project, projectMember, "디자인 진행 회의");
        ChatRoom latestOpenRoom = createRoom(project, projectMember, "최근 진행 회의");
        ChatRoom closedRoom = createRoom(project, projectMember, "디자인 종료 회의");
        ChatRoom otherRoom = createRoom(otherProject, otherProjectMember, "디자인 종료 회의");
        closedRoom.close(projectMember, LocalDateTime.of(2026, 10, 10, 14, 0));
        otherRoom.close(otherProjectMember, LocalDateTime.of(2026, 10, 10, 14, 0));
        chatRoomRepository.saveAllAndFlush(java.util.List.of(closedRoom, otherRoom));

        mockMvc.perform(get("/chat-rooms")
                        .header(MEMBER_ID_HEADER, member.getId())
                        .param("status", "CLOSED")
                        .param("projectId", project.getId().toString())
                        .param("query", "종료"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].chatRoomId").value(closedRoom.getId()))
                .andExpect(jsonPath("$.data.content[0].status").value("CLOSED"))
                .andExpect(jsonPath("$.data.content[0].participantCount").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        mockMvc.perform(get("/chat-rooms")
                        .header(MEMBER_ID_HEADER, member.getId())
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].chatRoomId")
                        .value(latestOpenRoom.getId()))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value(latestOpenRoom.getId()));

        mockMvc.perform(get("/chat-rooms")
                        .header(MEMBER_ID_HEADER, member.getId())
                        .param("cursor", latestOpenRoom.getId().toString())
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].chatRoomId").value(openRoom.getId()))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    void blocksChatRoomDetailForNonParticipant() throws Exception {
        Project project = createProject("프로젝트 루프");
        Member creatorMember = createMember("detail-creator");
        ProjectMember creator = createProjectMember(
                project,
                creatorMember,
                ProjectMemberStatus.JOINED
        );
        Member outsiderMember = createMember("detail-outsider");
        createProjectMember(project, outsiderMember, ProjectMemberStatus.JOINED);
        flush();
        ChatRoom chatRoom = createRoom(project, creator, "참여자 전용 채팅");

        mockMvc.perform(get("/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .header(MEMBER_ID_HEADER, outsiderMember.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_MEMBER_REQUIRED"));
    }

    private ChatRoom createRoom(
            Project project,
            ProjectMember creator,
            String name
    ) {
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                ChatRoom.create(project, null, creator, name)
        );
        chatRoomMemberRepository.saveAndFlush(ChatRoomMember.create(chatRoom, creator));
        return chatRoom;
    }

    private Project createProject(String name) {
        Project project = instantiate(Project.class);
        ReflectionTestUtils.setField(project, "name", name);
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
            ProjectMemberStatus status
    ) {
        ProjectMember projectMember = instantiate(ProjectMember.class);
        ReflectionTestUtils.setField(projectMember, "project", project);
        ReflectionTestUtils.setField(projectMember, "member", member);
        ReflectionTestUtils.setField(projectMember, "status", status);
        entityManager.persist(projectMember);
        return projectMember;
    }

    private Schedule createSchedule(Project project, Member creator) {
        Schedule schedule = instantiate(Schedule.class);
        ReflectionTestUtils.setField(schedule, "project", project);
        ReflectionTestUtils.setField(schedule, "creator", creator);
        ReflectionTestUtils.setField(schedule, "title", "공유 일정");
        ReflectionTestUtils.setField(schedule, "startAt", LocalDateTime.of(2026, 10, 10, 14, 0));
        ReflectionTestUtils.setField(schedule, "endAt", LocalDateTime.of(2026, 10, 10, 15, 0));
        ReflectionTestUtils.setField(schedule, "shared", true);
        entityManager.persist(schedule);
        return schedule;
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
