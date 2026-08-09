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
import com.yashwardhanv.linepilot.exception.ResourceNotFoundException;
import com.yashwardhanv.linepilot.repository.QueueTokenRepository;
import com.yashwardhanv.linepilot.repository.ServiceQueueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class QueueQueryService {

    private static final List<TokenStatus> ACTIVE_STATUSES =
            List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING);
    private static final List<TokenStatus> CLAIMED_STATUSES =
            List.of(TokenStatus.CALLED, TokenStatus.SERVING);

    private final ServiceQueueRepository queueRepository;
    private final QueueTokenRepository tokenRepository;
    private final WaitTimeEstimator waitTimeEstimator;
    private final Clock clock;

    public QueueQueryService(ServiceQueueRepository queueRepository,
                             QueueTokenRepository tokenRepository,
                             WaitTimeEstimator waitTimeEstimator,
                             Clock clock) {
        this.queueRepository = queueRepository;
        this.tokenRepository = tokenRepository;
        this.waitTimeEstimator = waitTimeEstimator;
        this.clock = clock;
    }

    public List<QueueSummaryResponse> listQueues() {
        return queueRepository.findAllByOrderByNameAsc().stream()
                .map(this::toSummary)
                .toList();
    }

    public QueueSnapshotResponse snapshot(Long queueId) {
        ServiceQueue queue = findQueue(queueId);
        List<QueueToken> tokens = tokenRepository
                .findByServiceQueueIdAndStatusInOrderByPriorityDescJoinedAtAscIdAsc(queueId, ACTIVE_STATUSES);
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

    public TokenResponse findPublicToken(UUID publicId) {
        return toTokenResponse(tokenRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue token not found")));
    }

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

    public ServiceQueue findQueue(Long queueId) {
        return queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue " + queueId + " not found"));
    }

    public TokenResponse toTokenResponse(QueueToken token) {
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

    private long peopleAhead(QueueToken token) {
        if (token.getStatus() != TokenStatus.WAITING) {
            return 0;
        }
        long claimed = tokenRepository.countByServiceQueueIdAndStatusIn(
                token.getServiceQueue().getId(), CLAIMED_STATUSES);
        List<QueueToken> waiting = tokenRepository
                .findByServiceQueueIdAndStatusInOrderByPriorityDescJoinedAtAscIdAsc(
                        token.getServiceQueue().getId(), List.of(TokenStatus.WAITING));
        int index = 0;
        for (QueueToken item : waiting) {
            if (item.getId().equals(token.getId())) {
                return claimed + index;
            }
            index++;
        }
        return 0;
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
