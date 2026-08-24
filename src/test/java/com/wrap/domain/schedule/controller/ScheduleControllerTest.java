package com.wrap.domain.schedule.controller;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.schedule.dto.ScheduleCreateRequest;
import com.wrap.domain.schedule.dto.ScheduleDetailResponse;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.service.ScheduleService;
import com.wrap.global.security.MemberDetails;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScheduleControllerTest {

    private ScheduleService scheduleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        scheduleService = Mockito.mock(ScheduleService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ScheduleController(scheduleService))
                .setCustomArgumentResolvers(memberDetailsResolver(1L))
                .build();
    }

    @Test
    void createScheduleUsesAuthenticatedMemberId() throws Exception {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 23, 14, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 23, 15, 0);
        ScheduleResponse response = new ScheduleResponse(
                1L,
                10L,
                1L,
                "Final poster meeting",
                "Review final poster draft.",
                startAt,
                endAt,
                true
        );

        when(scheduleService.create(eq(1L), any(ScheduleCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": 10,
                                  "title": "Final poster meeting",
                                  "description": "Review final poster draft.",
                                  "startAt": "2026-07-23T14:00:00",
                                  "endAt": "2026-07-23T15:00:00",
                                  "shared": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.creatorId").value(1));

        verify(scheduleService).create(eq(1L), any(ScheduleCreateRequest.class));
    }

    @Test
    void findDetailUsesAuthenticatedMemberId() throws Exception {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 23, 14, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 23, 15, 0);
        ScheduleDetailResponse response = new ScheduleDetailResponse(
                1L,
                null,
                null,
                1L,
                "member",
                "My schedule",
                null,
                startAt,
                endAt,
                false,
                true
        );
        when(scheduleService.findDetail(1L, 1L)).thenReturn(response);

        mockMvc.perform(get("/schedules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checked").value(true));

        verify(scheduleService).findDetail(1L, 1L);
    }

    @Test
    void checkScheduleUsesAuthenticatedMemberId() throws Exception {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 23, 14, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 23, 15, 0);
        ScheduleDetailResponse response = new ScheduleDetailResponse(
                1L,
                null,
                null,
                1L,
                "member",
                "My schedule",
                null,
                startAt,
                endAt,
                false,
                true
        );
        when(scheduleService.check(1L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/schedules/1/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checked").value(true));

        verify(scheduleService).check(1L, 1L);
    }

    private HandlerMethodArgumentResolver memberDetailsResolver(Long memberId) {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return MemberDetails.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                Member member = new MemberFixture(memberId).member();
                return new MemberDetails(member);
            }
        };
    }

    private static class MemberFixture {

        private final Long memberId;

        private MemberFixture(Long memberId) {
            this.memberId = memberId;
        }

        private Member member() {
            Member member = instantiate(Member.class);
            ReflectionTestUtils.setField(member, "id", memberId);
            return member;
        }

        private <T> T instantiate(Class<T> type) {
            try {
                var constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to instantiate test entity.", exception);
            }
        }
    }
}
