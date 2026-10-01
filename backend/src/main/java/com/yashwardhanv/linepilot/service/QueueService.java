package com.yashwardhanv.linepilot.service;

import com.yashwardhanv.linepilot.dto.HistoryItemResponse;
import com.yashwardhanv.linepilot.dto.HistoryPageResponse;
import com.yashwardhanv.linepilot.dto.QueueSnapshotResponse;
import com.yashwardhanv.linepilot.dto.QueueSummaryResponse;
import com.yashwardhanv.linepilot.dto.QueueTokenView;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * All queue business logic. Write methods are @Transactional; read methods are
 * @Transactional(readOnly = true). Controllers notify live boards after these
 * methods return, i.e. after the transaction has committed.
 */
@Service
public class QueueService {

    private static final List<TokenStatus> ACTIVE_STATUSES =
            List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING);
    private static final List<TokenStatus> CLAIMED_STATUSES =
            List.of(TokenStatus.CALLED, TokenStatus.SERVING);

    private final ServiceQueueRepository queueRepository;
    private final QueueTokenRepository tokenRepository;
    private final UserAccountRepository userRepository;
    private final WaitTimeEstimator waitTimeEstimator;
    private final Clock clock;

    public QueueService(ServiceQueueRepository queueRepository,
                        QueueTokenRepository tokenRepository,
                        UserAccountRepository userRepository,
                        WaitTimeEstimator waitTimeEstimator,
                        Clock clock) {
        this.queueRepository = queueRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.waitTimeEstimator = waitTimeEstimator;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- customer actions

    @Transactional
    public TokenResponse joinQueue(Long queueId, String customerName) {
        // Lock the queue row so two customers joining at once cannot get the same number.
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
        return toTokenResponse(token);
    }

    @Transactional
    public TokenResponse cancel(UUID publicId) {
        QueueToken token = tokenRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue token not found"));
        token.cancel(clock);
        tokenRepository.save(token);
        return toTokenResponse(token);
    }

    // ---------------------------------------------------------------- staff actions

    @Transactional
    public TokenResponse callNext(Long queueId, String username) {
        findQueue(queueId);
        UserAccount staff = findUser(username);
        tokenRepository.findFirstByClaimedByUsernameAndStatusIn(username, CLAIMED_STATUSES)
                .ifPresent(token -> {
                    throw new ConflictException("Complete or skip " + token.getDisplayNumber() + " before calling another token");
                });

        // SELECT ... FOR UPDATE SKIP LOCKED: two staff calling at once always get different tokens.
        QueueToken token = tokenRepository.lockNextWaitingToken(queueId)
                .orElseThrow(() -> new ConflictException("No waiting tokens are available"));
        token.call(staff, clock);
        tokenRepository.saveAndFlush(token);
        return toTokenResponse(token);
    }

    @Transactional
    public TokenResponse startServing(Long tokenId, String username) {
        UserAccount staff = findUser(username);
        QueueToken token = findAssignedToken(tokenId, username);
        token.startServing(staff, clock);
        return toTokenResponse(tokenRepository.save(token));
    }

    @Transactional
    public TokenResponse complete(Long tokenId, String username) {
        UserAccount staff = findUser(username);
        QueueToken token = findAssignedToken(tokenId, username);
        token.complete(staff, clock);
        return toTokenResponse(tokenRepository.save(token));
    }

    @Transactional
    public TokenResponse skip(Long tokenId, String username) {
        UserAccount staff = findUser(username);
        QueueToken token = findAssignedToken(tokenId, username);
        token.skip(staff, clock);
        return toTokenResponse(tokenRepository.save(token));
    }

    // ---------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public List<QueueSummaryResponse> listQueues() {
        return queueRepository.findAllByOrderByNameAsc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public QueueSnapshotResponse snapshot(Long queueId) {
        ServiceQueue queue = findQueue(queueId);
        List<QueueToken> tokens = tokenRepository
                .findByServiceQueueIdAndStatusInOrderByJoinedAtAscIdAsc(queueId, ACTIVE_STATUSES);
        long waiting = tokens.stream().filter(token -> token.getStatus() == TokenStatus.WAITING).count();
        return new QueueSnapshotResponse(
                queue.getId(),
                queue.getName(),
                queue.getLocation(),
                queue.isOpen(),
                waiting,
                estimate(queue, waiting),
                clock.instant(),
                tokens.stream().map(this::toQueueTokenView).toList()
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse findPublicToken(UUID publicId) {
        return toTokenResponse(tokenRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue token not found")));
    }

    @Transactional(readOnly = true)
    public HistoryPageResponse history(Long queueId, TokenStatus status, int page, int size) {
        findQueue(queueId);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "joinedAt"));
        Page<QueueToken> result = status == null
                ? tokenRepository.findByServiceQueueId(queueId, pageRequest)
                : tokenRepository.findByServiceQueueIdAndStatus(queueId, status, pageRequest);

        return new HistoryPageResponse(
                result.getContent().stream().map(this::toHistoryItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ---------------------------------------------------------------- helpers

    private ServiceQueue findQueue(Long queueId) {
        return queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue " + queueId + " not found"));
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found"));
    }

    private QueueToken findAssignedToken(Long tokenId, String username) {
        return tokenRepository.findByIdAndClaimedByUsername(tokenId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Assigned queue token not found"));
    }

    private long peopleAhead(QueueToken token) {
        if (token.getStatus() != TokenStatus.WAITING) {
            return 0;
        }
        Long queueId = token.getServiceQueue().getId();
        // Everyone currently being called/served, plus everyone who joined before this token.
        return tokenRepository.countByServiceQueueIdAndStatusIn(queueId, CLAIMED_STATUSES)
                + tokenRepository.countWaitingAhead(queueId, token.getJoinedAt(), token.getId());
    }

    private long estimate(ServiceQueue queue, long peopleAhead) {
        return waitTimeEstimator.estimateMinutes(
                peopleAhead,
                recentServiceDurations(queue.getId()),
                queue.getDefaultServiceMinutes()
        );
    }

    private List<Duration> recentServiceDurations(Long queueId) {
        return tokenRepository
                .findTop10ByServiceQueueIdAndStatusAndServingAtIsNotNullOrderByCompletedAtDesc(
                        queueId, TokenStatus.COMPLETED)
                .stream()
                .filter(token -> token.getCompletedAt() != null)
                .map(token -> Duration.between(token.getServingAt(), token.getCompletedAt()))
                .filter(duration -> !duration.isNegative() && !duration.isZero())
                .toList();
    }

    // ---------------------------------------------------------------- entity -> DTO

    private TokenResponse toTokenResponse(QueueToken token) {
        long peopleAhead = peopleAhead(token);
        return new TokenResponse(
                token.getId(),
                token.getPublicId(),
                token.getServiceQueue().getId(),
                token.getServiceQueue().getName(),
                token.getDisplayNumber(),
                token.getCustomerName(),
                token.getStatus(),
                peopleAhead,
                estimate(token.getServiceQueue(), peopleAhead),
                token.getClaimedBy() == null ? null : token.getClaimedBy().getDisplayName(),
                token.getJoinedAt(),
                token.getUpdatedAt()
        );
    }

    private QueueSummaryResponse toSummary(ServiceQueue queue) {
        long waiting = tokenRepository.countByServiceQueueIdAndStatus(queue.getId(), TokenStatus.WAITING);
        return new QueueSummaryResponse(
                queue.getId(), queue.getCode(), queue.getName(), queue.getLocation(), queue.isOpen(),
                waiting, estimate(queue, waiting)
        );
    }

    private QueueTokenView toQueueTokenView(QueueToken token) {
        return new QueueTokenView(
                token.getId(),
                token.getDisplayNumber(),
                token.getStatus(),
                token.getClaimedBy() == null ? null : token.getClaimedBy().getDisplayName(),
                token.getJoinedAt(),
                token.getUpdatedAt()
        );
    }

    private HistoryItemResponse toHistoryItem(QueueToken token) {
        return new HistoryItemResponse(
                token.getId(), token.getDisplayNumber(), token.getCustomerName(), token.getStatus(),
                token.getClaimedBy() == null ? null : token.getClaimedBy().getDisplayName(),
                token.getJoinedAt(), token.getCalledAt(), token.getServingAt(), token.getCompletedAt()
        );
    }
}
