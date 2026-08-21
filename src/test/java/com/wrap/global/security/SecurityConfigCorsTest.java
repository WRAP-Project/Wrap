package com.wrap.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
        controllers = SecurityConfigCorsTest.CorsTestController.class,
        properties = "app.cors.allowed-origin=https://frontend.example.com"
)
@Import(SecurityConfig.class)
class SecurityConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsCredentialedPreflightRequestFromConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/cors-test")
                        .header(HttpHeaders.ORIGIN, "https://frontend.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://frontend.example.com"
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                        "true"
                ));
    }

    @Test
    void rejectsPreflightRequestFromUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/cors-test")
                        .header(HttpHeaders.ORIGIN, "https://malicious.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class CorsTestController {

        @GetMapping("/cors-test")
        String corsTest() {
            return "ok";
        }
    }
}
