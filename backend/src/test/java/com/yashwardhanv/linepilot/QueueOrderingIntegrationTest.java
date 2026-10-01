package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.dto.TokenResponse;
import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class QueueOrderingIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    QueueService queueService;

    @Test
    void tokensAreCalledInTheOrderCustomersJoined() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("ORDER", "Ordering", "Desk", "Q", 5));
        userRepository.save(new UserAccount("staff", "hash", "Staff"));
        TokenResponse first = queueService.joinQueue(queue.getId(), "First customer");
        TokenResponse second = queueService.joinQueue(queue.getId(), "Second customer");
        assertThat(first.peopleAhead()).isZero();
        assertThat(second.peopleAhead()).isEqualTo(1);

        TokenResponse called = queueService.callNext(queue.getId(), "staff");

        assertThat(called.id()).isEqualTo(first.id());
        assertThat(called.displayNumber()).isEqualTo("Q-001");
    }

    @Test
    void cancelledTokenCannotBeCalled() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("CANCEL", "Cancellation", "Desk", "C", 5));
        userRepository.save(new UserAccount("staff", "hash", "Staff"));
        TokenResponse first = queueService.joinQueue(queue.getId(), "Cancelling customer");
        TokenResponse second = queueService.joinQueue(queue.getId(), "Waiting customer");
        queueService.cancel(first.publicId());

        TokenResponse called = queueService.callNext(queue.getId(), "staff");

        assertThat(called.id()).isEqualTo(second.id());
        assertThat(tokenRepository.findByPublicId(first.publicId())).get()
                .extracting(token -> token.getStatus())
                .isEqualTo(TokenStatus.CANCELLED);
    }
}
