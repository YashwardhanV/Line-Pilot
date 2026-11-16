package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RateLimitIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    private ServiceQueue queue;

    @BeforeEach
    void setUp() {
        userRepository.save(new UserAccount("staff", passwordEncoder.encode("password"), "Staff"));
        queue = queueRepository.save(new ServiceQueue("RATE", "Rate", "Desk", "R", 5));
    }

    @Test
    void oneDeviceCannotFloodTheQueue() throws Exception {
        for (int i = 0; i < 5; i++) {
            join(from("10.0.0.1")).andExpect(status().isCreated());
        }
        join(from("10.0.0.1")).andExpect(status().isTooManyRequests());
        // Another customer is not affected.
        join(from("10.0.0.2")).andExpect(status().isCreated());
    }

    @Test
    void repeatedWrongPasswordsBlockThatIpEvenWithTheRightPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/auth/me").with(httpBasic("staff", "wrong")).with(from("10.0.0.3")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(get("/api/auth/me").with(httpBasic("staff", "password")).with(from("10.0.0.3")))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(get("/api/auth/me").with(httpBasic("staff", "password")).with(from("10.0.0.4")))
                .andExpect(status().isOk());
    }

    private ResultActions join(RequestPostProcessor ip) throws Exception {
        return mockMvc.perform(post("/api/queues/{queueId}/tokens", queue.getId())
                .with(ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Asha\"}"));
    }

    private RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
