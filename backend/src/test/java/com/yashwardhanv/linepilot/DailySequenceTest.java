package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.entity.ServiceQueue;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DailySequenceTest {

    @Test
    void numbersRestartAtLocalMidnightNotUtcMidnight() {
        ServiceQueue queue = new ServiceQueue("TEST", "Test", "Desk", "A", 5);
        ZoneId india = ZoneId.of("Asia/Kolkata");

        // 23:00 IST on the 16th is still the 16th locally.
        LocalDate lateEvening = LocalDate.now(Clock.fixed(Instant.parse("2026-11-16T17:30:00Z"), india));
        // 00:30 IST on the 17th is a new local day, even though it is still the 16th in UTC.
        LocalDate justAfterMidnight = LocalDate.now(Clock.fixed(Instant.parse("2026-11-16T19:00:00Z"), india));

        assertThat(queue.nextSequence(lateEvening)).isEqualTo(1);
        assertThat(queue.nextSequence(lateEvening)).isEqualTo(2);
        assertThat(queue.nextSequence(justAfterMidnight)).isEqualTo(1);
    }
}
