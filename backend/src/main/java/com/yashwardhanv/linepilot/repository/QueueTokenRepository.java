package com.yashwardhanv.linepilot.repository;

import com.yashwardhanv.linepilot.entity.QueueToken;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Tokens from an earlier day that were never served. version is bumped so a staff member or
    // customer acting on a stale copy of the row gets a 409 instead of overwriting the expiry.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE queue_tokens
               SET status = 'EXPIRED', completed_at = :now, updated_at = :now, version = version + 1
             WHERE service_date < :today
               AND status IN ('WAITING', 'CALLED')
            """, nativeQuery = true)
    int expireUnservedBefore(@Param("today") LocalDate today, @Param("now") Instant now);

    // Phone numbers are only needed on the day; drop them afterwards.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE queue_tokens SET customer_phone = NULL
             WHERE service_date < :today AND customer_phone IS NOT NULL
            """, nativeQuery = true)
    int clearPhonesBefore(@Param("today") LocalDate today);

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

    // Staff who are serving this queue now, or finished a customer here recently.
    @Query(value = """
            SELECT COUNT(DISTINCT claimed_by_id)
              FROM queue_tokens
             WHERE service_queue_id = :queueId
               AND claimed_by_id IS NOT NULL
               AND (status IN ('CALLED', 'SERVING')
                    OR (status IN ('COMPLETED', 'SKIPPED') AND completed_at >= :since))
            """, nativeQuery = true)
    long countActiveStaff(@Param("queueId") Long queueId, @Param("since") Instant since);

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
