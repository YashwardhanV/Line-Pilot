package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.entity.QueueToken;
import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.entity.UserRole;
import com.yashwardhanv.linepilot.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueTokenStateTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-07T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void followsExplicitHappyPathLifecycle() {
        UserAccount staff = new UserAccount("staff", "hash", "Staff", UserRole.STAFF);
        QueueToken token = token();

        token.call(staff, clock);
        token.startServing(staff, clock);
        token.complete(staff, clock);

        assertThat(token.getStatus()).isEqualTo(TokenStatus.COMPLETED);
        assertThat(token.getCalledAt()).isNotNull();
        assertThat(token.getServingAt()).isNotNull();
        assertThat(token.getCompletedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidTransition() {
        QueueToken token = token();
        token.cancel(clock);

        assertThatThrownBy(() -> token.cancel(clock))
                .isInstanceOf(ConflictException.class);
    }

    private QueueToken token() {
        ServiceQueue queue = new ServiceQueue("TEST", "Test", "Desk", "T", 5);
        return new QueueToken(queue, 1, LocalDate.of(2026, 8, 7), "T-001", "Customer", 0, clock);
    }
}
