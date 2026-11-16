package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.dto.TokenResponse;
import com.yashwardhanv.linepilot.entity.QueueToken;
import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TokenExpiryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    QueueService queueService;

    @Autowired
    Clock clock;

    @Test
    void yesterdaysUnservedTokensExpireAndDoNotBlockToday() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("EXP", "Expiry", "Desk", "E", 5));
        UserAccount staff = userRepository.save(new UserAccount("staff", "hash", "Staff"));
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);

        QueueToken leftWaiting = tokenRepository.save(
                new QueueToken(queue, 1, yesterday, "E-001", "Left waiting", clock));
        QueueToken leftCalled = new QueueToken(queue, 2, yesterday, "E-002", "Left called", clock);
        leftCalled.call(staff, clock);
        tokenRepository.save(leftCalled);
        TokenResponse today = queueService.joinQueue(queue.getId(), "Today's customer");

        assertThat(queueService.expireTokensFromEarlierDays()).isEqualTo(2);

        assertThat(tokenRepository.findById(leftWaiting.getId())).get()
                .extracting(QueueToken::getStatus).isEqualTo(TokenStatus.EXPIRED);
        assertThat(tokenRepository.findById(leftCalled.getId())).get()
                .extracting(QueueToken::getStatus).isEqualTo(TokenStatus.EXPIRED);
        // The staff member who was stuck holding yesterday's token can call today's customer.
        assertThat(queueService.callNext(queue.getId(), "staff").id()).isEqualTo(today.id());
    }
}
