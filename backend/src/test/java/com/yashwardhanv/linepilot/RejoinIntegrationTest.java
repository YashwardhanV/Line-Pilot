package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.dto.TokenResponse;
import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.exception.ConflictException;
import com.yashwardhanv.linepilot.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RejoinIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    QueueService queueService;

    @Test
    void noShowCanRejoinOnceAtTheBackOfTheLine() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("REJ", "Rejoin", "Desk", "R", 5));
        userRepository.save(new UserAccount("staff", "hash", "Staff"));
        TokenResponse late = queueService.joinQueue(queue.getId(), "Late customer", "+919876543210");
        queueService.joinQueue(queue.getId(), "On-time customer");
        queueService.skip(queueService.callNext(queue.getId(), "staff").id(), "staff");

        TokenResponse back = queueService.rejoin(late.publicId());

        assertThat(back.displayNumber()).isEqualTo("R-003");
        assertThat(back.status()).isEqualTo(TokenStatus.WAITING);
        assertThat(back.peopleAhead()).isEqualTo(1); // behind the on-time customer
        assertThat(tokenRepository.findById(back.id())).get()
                .satisfies(token -> assertThat(token.getCustomerPhone()).isEqualTo("+919876543210"));
        assertThatThrownBy(() -> queueService.rejoin(late.publicId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been put back");
    }

    @Test
    void onlySkippedTokensCanRejoin() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("REJ2", "Rejoin", "Desk", "Q", 5));
        TokenResponse waiting = queueService.joinQueue(queue.getId(), "Customer");

        assertThatThrownBy(() -> queueService.rejoin(waiting.publicId()))
                .isInstanceOf(ConflictException.class);
    }
}
