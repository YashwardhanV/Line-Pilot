package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.entity.QueueToken;
import com.yashwardhanv.linepilot.entity.ServiceQueue;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueRepositoryConstraintIntegrationTest extends PostgresIntegrationTest {

    @Test
    void databaseRejectsDuplicateDailySequenceForAQueue() {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("UNIQUE", "Unique", "Desk", "U", 5));
        LocalDate today = LocalDate.now(Clock.systemUTC());
        tokenRepository.saveAndFlush(new QueueToken(
                queue, 1, today, "U-001", "First", 0, Clock.systemUTC()));

        assertThatThrownBy(() -> tokenRepository.saveAndFlush(new QueueToken(
                queue, 1, today, "U-001", "Second", 0, Clock.systemUTC())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
