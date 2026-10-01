package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.dto.TokenResponse;
import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.service.QueueCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class QueueOrderingIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    QueueCommandService commandService;

    @Test
    void tokensAreCalledInTheOrderCustomersJoined() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("ORDER", "Ordering", "Desk", "Q", 5));
        userRepository.save(new UserAccount("staff", "hash", "Staff"));
        TokenResponse first = commandService.joinQueue(queue.getId(), "First customer");
        commandService.joinQueue(queue.getId(), "Second customer");

        TokenResponse called = commandService.callNext(queue.getId(), "staff");

        assertThat(called.id()).isEqualTo(first.id());
        assertThat(called.displayNumber()).isEqualTo("Q-001");
    }

    @Test
    void cancelledTokenCannotBeCalled() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("CANCEL", "Cancellation", "Desk", "C", 5));
        userRepository.save(new UserAccount("staff", "hash", "Staff"));
        TokenResponse first = commandService.joinQueue(queue.getId(), "Cancelling customer");
        TokenResponse second = commandService.joinQueue(queue.getId(), "Waiting customer");
        commandService.cancel(first.publicId());

        TokenResponse called = commandService.callNext(queue.getId(), "staff");

        assertThat(called.id()).isEqualTo(second.id());
        assertThat(tokenRepository.findByPublicId(first.publicId())).get()
                .extracting(token -> token.getStatus())
                .isEqualTo(TokenStatus.CANCELLED);
    }
}
