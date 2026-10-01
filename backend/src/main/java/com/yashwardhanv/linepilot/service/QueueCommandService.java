package com.yashwardhanv.linepilot.service;

import com.yashwardhanv.linepilot.dto.QueueRequest;
import com.yashwardhanv.linepilot.dto.QueueSummaryResponse;
import com.yashwardhanv.linepilot.dto.TokenResponse;
import com.yashwardhanv.linepilot.entity.QueueToken;
import com.yashwardhanv.linepilot.entity.ServiceQueue;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import com.yashwardhanv.linepilot.entity.UserAccount;
import com.yashwardhanv.linepilot.exception.ConflictException;
import com.yashwardhanv.linepilot.exception.ResourceNotFoundException;
import com.yashwardhanv.linepilot.repository.QueueTokenRepository;
import com.yashwardhanv.linepilot.repository.ServiceQueueRepository;
import com.yashwardhanv.linepilot.repository.UserAccountRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class QueueCommandService {

    private static final List<TokenStatus> ACTIVE_CLAIM_STATUSES =
            List.of(TokenStatus.CALLED, TokenStatus.SERVING);

    private final ServiceQueueRepository queueRepository;
    private final QueueTokenRepository tokenRepository;
    private final UserAccountRepository userRepository;
    private final QueueQueryService queryService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public QueueCommandService(ServiceQueueRepository queueRepository,
                               QueueTokenRepository tokenRepository,
                               UserAccountRepository userRepository,
                               QueueQueryService queryService,
                               ApplicationEventPublisher eventPublisher,
                               Clock clock) {
        this.queueRepository = queueRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.queryService = queryService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public TokenResponse joinQueue(Long queueId, String customerName) {
        ServiceQueue queue = queueRepository.findByIdForUpdate(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue " + queueId + " not found"));
        if (!queue.isOpen()) {
            throw new ConflictException("This queue is currently closed");
        }

        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        int sequence = queue.nextSequence(today);
        String displayNumber = queue.getTokenPrefix().toUpperCase(Locale.ROOT)
                + "-" + String.format(Locale.ROOT, "%03d", sequence);
        QueueToken token = new QueueToken(
                queue, sequence, today, displayNumber, customerName.strip(), clock);
        tokenRepository.saveAndFlush(token);
        changed(queueId);
        return queryService.toTokenResponse(token);
    }

    @Transactional
    public TokenResponse cancel(UUID publicId) {
        QueueToken token = tokenRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue token not found"));
        token.cancel(clock);
        tokenRepository.save(token);
        changed(token.getServiceQueue().getId());
        return queryService.toTokenResponse(token);
    }

    @Transactional
    public TokenResponse callNext(Long queueId, String username) {
        ServiceQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue " + queueId + " not found"));
        UserAccount staff = findUser(username);
        tokenRepository.findFirstByClaimedByUsernameAndStatusIn(username, ACTIVE_CLAIM_STATUSES)
                .ifPresent(token -> {
                    throw new ConflictException("Complete or skip " + token.getDisplayNumber() + " before calling another token");
                });

        QueueToken token = tokenRepository.lockNextWaitingToken(queue.getId())
                .orElseThrow(() -> new ConflictException("No waiting tokens are available"));
        token.call(staff, clock);
        tokenRepository.saveAndFlush(token);
        changed(queueId);
        return queryService.toTokenResponse(token);
    }

    @Transactional
    public TokenResponse startServing(Long tokenId, String username) {
        return transition(tokenId, username, Transition.START);
    }

    @Transactional
    public TokenResponse complete(Long tokenId, String username) {
        return transition(tokenId, username, Transition.COMPLETE);
    }

    @Transactional
    public TokenResponse skip(Long tokenId, String username) {
        return transition(tokenId, username, Transition.SKIP);
    }

    @Transactional
    public QueueSummaryResponse createQueue(QueueRequest request) {
        if (queueRepository.existsByCode(request.code())) {
            throw new ConflictException("Queue code already exists");
        }
        ServiceQueue queue = new ServiceQueue(
                request.code(), request.name(), request.location(), request.tokenPrefix(),
                request.defaultServiceMinutes());
        queue.update(request.name(), request.location(), request.open(), request.defaultServiceMinutes());
        queueRepository.save(queue);
        return queryService.listQueues().stream()
                .filter(item -> item.code().equals(request.code()))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public QueueSummaryResponse updateQueue(Long queueId, QueueRequest request) {
        ServiceQueue queue = queueRepository.findByIdForUpdate(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue " + queueId + " not found"));
        if (!queue.getCode().equals(request.code()) || !queue.getTokenPrefix().equals(request.tokenPrefix())) {
            throw new ConflictException("Queue code and token prefix cannot be changed after creation");
        }
        queue.update(request.name(), request.location(), request.open(), request.defaultServiceMinutes());
        changed(queueId);
        return queryService.listQueues().stream()
                .filter(item -> item.id().equals(queueId))
                .findFirst()
                .orElseThrow();
    }

    private TokenResponse transition(Long tokenId, String username, Transition transition) {
        UserAccount staff = findUser(username);
        QueueToken token = tokenRepository.findByIdAndClaimedByUsername(tokenId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Assigned queue token not found"));
        switch (transition) {
            case START -> token.startServing(staff, clock);
            case COMPLETE -> token.complete(staff, clock);
            case SKIP -> token.skip(staff, clock);
        }
        tokenRepository.save(token);
        changed(token.getServiceQueue().getId());
        return queryService.toTokenResponse(token);
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found"));
    }

    private void changed(Long queueId) {
        eventPublisher.publishEvent(new QueueChangedEvent(queueId, clock.instant()));
    }

    private enum Transition {
        START, COMPLETE, SKIP
    }
}
