package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.entity.QueueToken;
import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.notify.CustomerNotifier;
import com.yashwardhanv.linepilot.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class NotificationIntegrationTest extends PostgresIntegrationTest {

    @MockitoBean
    CustomerNotifier notifier;

    @Autowired
    QueueService queueService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    Clock clock;

    @Test
    void customerWithAPhoneIsToldWhichCounterToGoTo() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("SMS", "Citizen Desk", "Hall", "S", 5));
        userRepository.save(new UserAccount("staff", "hash", "Staff", "Counter 4"));
        queueService.joinQueue(queue.getId(), "Asha", "+919876543210");

        queueService.callNext(queue.getId(), "staff");

        verify(notifier, timeout(5000)).send(eq("+919876543210"), contains("S-001 is being called at Counter 4"));
    }

    @Test
    void customerWithoutAPhoneIsNotMessaged() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("NOSMS", "Desk", "Hall", "N", 5));
        userRepository.save(new UserAccount("staff", "hash", "Staff"));
        queueService.joinQueue(queue.getId(), "Asha");

        queueService.callNext(queue.getId(), "staff");

        verify(notifier, after(500).never()).send(anyString(), anyString());
    }

    @Test
    void invalidPhoneIsRejected() throws Exception {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("BAD", "Desk", "Hall", "B", 5));

        mockMvc.perform(post("/api/queues/{queueId}/tokens", queue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Asha\",\"customerPhone\":\"98765\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerPhone").exists());
    }

    @Test
    void phoneNumbersAreDeletedAfterTheDay() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("OLD", "Desk", "Hall", "O", 5));
        QueueToken old = tokenRepository.save(new QueueToken(
                queue, 1, LocalDate.now(clock).minusDays(1), "O-001", "Asha", "+919876543210", clock));

        queueService.expireTokensFromEarlierDays();

        assertThat(tokenRepository.findById(old.getId())).get()
                .satisfies(token -> assertThat(token.getCustomerPhone()).isNull());
    }
}
