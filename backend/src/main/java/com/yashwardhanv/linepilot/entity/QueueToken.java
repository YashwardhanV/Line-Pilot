package com.yashwardhanv.linepilot.entity;

import com.yashwardhanv.linepilot.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "queue_tokens")
public class QueueToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_queue_id", nullable = false)
    private ServiceQueue serviceQueue;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "display_number", nullable = false, length = 20)
    private String displayNumber;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenStatus status;

    @Column(nullable = false)
    private int priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by_id")
    private UserAccount claimedBy;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "serving_at")
    private Instant servingAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected QueueToken() {
    }

    public QueueToken(ServiceQueue queue, int sequenceNumber, LocalDate serviceDate,
                      String displayNumber, String customerName, int priority, Clock clock) {
        this.publicId = UUID.randomUUID();
        this.serviceQueue = queue;
        this.sequenceNumber = sequenceNumber;
        this.serviceDate = serviceDate;
        this.displayNumber = displayNumber;
        this.customerName = customerName;
        this.status = TokenStatus.WAITING;
        this.priority = priority;
        this.joinedAt = clock.instant();
        this.updatedAt = joinedAt;
    }

    public void call(UserAccount staff, Clock clock) {
        requireStatus(TokenStatus.WAITING);
        status = TokenStatus.CALLED;
        claimedBy = staff;
        calledAt = clock.instant();
        updatedAt = calledAt;
    }

    public void startServing(UserAccount staff, Clock clock) {
        requireClaim(staff);
        requireStatus(TokenStatus.CALLED);
        status = TokenStatus.SERVING;
        servingAt = clock.instant();
        updatedAt = servingAt;
    }

    public void complete(UserAccount staff, Clock clock) {
        requireClaim(staff);
        requireStatus(TokenStatus.SERVING);
        status = TokenStatus.COMPLETED;
        completedAt = clock.instant();
        updatedAt = completedAt;
    }

    public void skip(UserAccount staff, Clock clock) {
        requireClaim(staff);
        requireStatus(TokenStatus.CALLED);
        status = TokenStatus.SKIPPED;
        completedAt = clock.instant();
        updatedAt = completedAt;
    }

    public void cancel(Clock clock) {
        requireStatus(TokenStatus.WAITING);
        status = TokenStatus.CANCELLED;
        completedAt = clock.instant();
        updatedAt = completedAt;
    }

    private void requireClaim(UserAccount staff) {
        boolean samePersistedUser = claimedBy != null
                && claimedBy.getId() != null
                && claimedBy.getId().equals(staff.getId());
        if (claimedBy == null || (claimedBy != staff && !samePersistedUser)) {
            throw new ConflictException("Token is assigned to a different staff member");
        }
    }

    private void requireStatus(TokenStatus expected) {
        if (status != expected) {
            throw new ConflictException("Expected token status " + expected + " but was " + status);
        }
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public ServiceQueue getServiceQueue() { return serviceQueue; }
    public int getSequenceNumber() { return sequenceNumber; }
    public LocalDate getServiceDate() { return serviceDate; }
    public String getDisplayNumber() { return displayNumber; }
    public String getCustomerName() { return customerName; }
    public TokenStatus getStatus() { return status; }
    public int getPriority() { return priority; }
    public UserAccount getClaimedBy() { return claimedBy; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getCalledAt() { return calledAt; }
    public Instant getServingAt() { return servingAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
