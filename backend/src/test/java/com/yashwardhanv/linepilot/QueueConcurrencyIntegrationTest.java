package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.dto.TokenResponse;
import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.exception.ConflictException;
import com.yashwardhanv.linepilot.service.QueueCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class QueueConcurrencyIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    QueueCommandService commandService;

    @Test
    void concurrentCallNextRequestsNeverClaimTheSameSingleToken() throws Exception {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("ONE", "Single token", "Desk", "S", 5));
        userRepository.save(new UserAccount("staff1", "hash", "Staff One"));
        userRepository.save(new UserAccount("staff2", "hash", "Staff Two"));
        commandService.joinQueue(queue.getId(), "Only Customer");

        List<CallResult> results = callConcurrently(queue.getId(), List.of("staff1", "staff2"));

        List<TokenResponse> successes = results.stream()
                .filter(result -> result.token() != null)
                .map(CallResult::token)
                .toList();
        assertThat(successes).hasSize(1);
        assertThat(results.stream().filter(result -> result.error() instanceof ConflictException).count())
                .isEqualTo(1);
        assertThat(tokenRepository.findAll()).singleElement()
                .satisfies(token -> {
                    assertThat(token.getStatus()).isEqualTo(TokenStatus.CALLED);
                    assertThat(token.getClaimedBy()).isNotNull();
                });
    }

    @Test
    void concurrentStaffClaimDistinctTokensWhenTwoAreWaiting() throws Exception {
        ServiceQueue queue = queueRepository.save(new ServiceQueue("TWO", "Two tokens", "Desk", "T", 5));
        userRepository.save(new UserAccount("staff1", "hash", "Staff One"));
        userRepository.save(new UserAccount("staff2", "hash", "Staff Two"));
        commandService.joinQueue(queue.getId(), "First Customer");
        commandService.joinQueue(queue.getId(), "Second Customer");

        List<CallResult> results = callConcurrently(queue.getId(), List.of("staff1", "staff2"));

        assertThat(results).allSatisfy(result -> assertThat(result.error()).isNull());
        assertThat(results.stream().map(result -> result.token().id()).distinct()).hasSize(2);
        assertThat(tokenRepository.findAll()).allMatch(token -> token.getStatus() == TokenStatus.CALLED);
    }

    private List<CallResult> callConcurrently(Long queueId, List<String> usernames) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(usernames.size());
        CountDownLatch ready = new CountDownLatch(usernames.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<CallResult>> futures = new ArrayList<>();
            for (String username : usernames) {
                Callable<CallResult> task = () -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        return new CallResult(null, new IllegalStateException("Start latch timed out"));
                    }
                    try {
                        return new CallResult(commandService.callNext(queueId, username), null);
                    } catch (RuntimeException exception) {
                        return new CallResult(null, exception);
                    }
                };
                futures.add(executor.submit(task));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<CallResult> results = new ArrayList<>();
            for (Future<CallResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private record CallResult(TokenResponse token, RuntimeException error) {
    }
}
