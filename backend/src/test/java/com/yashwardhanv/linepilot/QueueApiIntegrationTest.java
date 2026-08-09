package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.entity.UserRole;
import com.yashwardhanv.linepilot.service.QueueCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class QueueApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    QueueCommandService commandService;

    private ServiceQueue queue;

    @BeforeEach
    void setUpApiData() {
        userRepository.save(new UserAccount(
                "staff", passwordEncoder.encode("password"), "Staff", UserRole.STAFF));
        userRepository.save(new UserAccount(
                "admin", passwordEncoder.encode("password"), "Admin", UserRole.ADMIN));
        queue = queueRepository.save(new ServiceQueue("API", "API Queue", "Desk", "A", 5));
    }

    @Test
    void publicCanListAndJoinQueues() throws Exception {
        mockMvc.perform(get("/api/queues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("API"));

        mockMvc.perform(post("/api/queues/{queueId}/tokens", queue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Asha\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayNumber").value("A-001"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void joinValidationReturnsStructuredBadRequest() throws Exception {
        mockMvc.perform(post("/api/queues/{queueId}/tokens", queue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.customerName").exists());
    }

    @Test
    void callNextRequiresAuthenticationAndAcceptsStaffRole() throws Exception {
        commandService.joinQueue(queue.getId(), "Customer");

        mockMvc.perform(post("/api/staff/queues/{queueId}/call-next", queue.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/staff/queues/{queueId}/call-next", queue.getId())
                        .with(httpBasic("staff", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.claimedBy").value("Staff"));
    }

    @Test
    void staffCannotUseAdminQueueManagementEndpoint() throws Exception {
        mockMvc.perform(post("/api/admin/queues")
                        .with(httpBasic("staff", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"NEW_QUEUE",
                                  "name":"New queue",
                                  "location":"Desk 2",
                                  "tokenPrefix":"N",
                                  "open":true,
                                  "defaultServiceMinutes":5
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void badCredentialsAreRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(httpBasic("staff", "wrong")))
                .andExpect(status().isUnauthorized());
    }
}
