package com.wrap.domain.chat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ChatOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesEveryChatEndpointInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/projects/{projectId}/chat-rooms'].post")
                        .exists())
                .andExpect(jsonPath("$.paths['/chat-rooms'].get").exists())
                .andExpect(jsonPath("$.paths['/chat-rooms/{chatRoomId}'].get").exists())
                .andExpect(jsonPath("$.paths['/chat-rooms/{chatRoomId}'].patch").exists())
                .andExpect(jsonPath("$.paths['/chat-rooms/{chatRoomId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/chat-rooms/{chatRoomId}/close'].patch")
                        .exists())
                .andExpect(jsonPath("$.paths['/chat-rooms/{chatRoomId}/members'].get")
                        .exists())
                .andExpect(jsonPath("$.paths['/chat-rooms/{chatRoomId}/messages'].post")
                        .exists())
                .andExpect(jsonPath("$.paths['/chat-rooms/{chatRoomId}/messages'].get")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/chat-rooms/{chatRoomId}/messages/{messageId}'].patch"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/chat-rooms/{chatRoomId}/messages/{messageId}'].delete"
                ).exists())
                .andExpect(jsonPath("$.paths['/chat-rooms/{chatRoomId}/read'].put")
                        .exists());
    }
}
