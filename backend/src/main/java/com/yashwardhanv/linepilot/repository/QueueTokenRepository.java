package com.yashwardhanv.linepilot.repository;

import com.yashwardhanv.linepilot.entity.QueueToken;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {

    Optional<QueueToken> findByPublicId(UUID publicId);

    long countByServiceQueueIdAndStatus(Long queueId, TokenStatus status);

    long countByServiceQueueIdAndStatusIn(Long queueId, Collection<TokenStatus> statuses);

    long countByServiceQueueIdAndServiceDate(Long queueId, LocalDate serviceDate);

    @Query(value = """
            SELECT *
              FROM queue_tokens
             WHERE service_queue_id = :queueId
               AND status = 'WAITING'
             ORDER BY joined_at ASC, id ASC
             FOR UPDATE SKIP LOCKED
             LIMIT 1
            """, nativeQuery = true)
    Optional<QueueToken> lockNextWaitingToken(@Param("queueId") Long queueId);

    // Waiting tokens that joined before the given token (ties broken by id).
    @Query(value = """
            SELECT COUNT(*)
              FROM queue_tokens
             WHERE service_queue_id = :queueId
               AND status = 'WAITING'
               AND id <> :tokenId
               AND (joined_at < :joinedAt OR (joined_at = :joinedAt AND id < :tokenId))
            """, nativeQuery = true)
    long countWaitingAhead(@Param("queueId") Long queueId,
                           @Param("joinedAt") Instant joinedAt,
                           @Param("tokenId") Long tokenId);

    @EntityGraph(attributePaths = {"claimedBy"})
    Optional<QueueToken> findByIdAndClaimedByUsername(Long tokenId, String username);

    @EntityGraph(attributePaths = {"serviceQueue", "claimedBy"})
    Optional<QueueToken> findFirstByClaimedByUsernameAndStatusIn(
            String username, Collection<TokenStatus> statuses);

    @EntityGraph(attributePaths = {"claimedBy"})
    List<QueueToken> findByServiceQueueIdAndStatusInOrderByJoinedAtAscIdAsc(
            Long queueId, Collection<TokenStatus> statuses);

    @EntityGraph(attributePaths = {"claimedBy"})
    List<QueueToken> findTop10ByServiceQueueIdAndStatusAndServingAtIsNotNullOrderByCompletedAtDesc(
            Long queueId, TokenStatus status);

    @EntityGraph(attributePaths = {"claimedBy"})
    Page<QueueToken> findByServiceQueueId(Long queueId, Pageable pageable);

    @EntityGraph(attributePaths = {"claimedBy"})
    Page<QueueToken> findByServiceQueueIdAndStatus(Long queueId, TokenStatus status, Pageable pageable);
}
