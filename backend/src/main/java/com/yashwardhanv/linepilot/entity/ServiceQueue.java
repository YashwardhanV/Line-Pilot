package com.yashwardhanv.linepilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "service_queues")
public class ServiceQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 160)
    private String location;

    @Column(name = "token_prefix", nullable = false, length = 5)
    private String tokenPrefix;

    @Column(nullable = false)
    private boolean open = true;

    @Column(name = "default_service_minutes", nullable = false)
    private int defaultServiceMinutes;

    @Column(name = "sequence_date")
    private LocalDate sequenceDate;

    @Column(name = "last_sequence", nullable = false)
    private int lastSequence;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ServiceQueue() {
    }

    public ServiceQueue(String code, String name, String location, String tokenPrefix, int defaultServiceMinutes) {
        this.code = code;
        this.name = name;
        this.location = location;
        this.tokenPrefix = tokenPrefix;
        this.defaultServiceMinutes = defaultServiceMinutes;
    }

    public int nextSequence(LocalDate today) {
        if (!today.equals(sequenceDate)) {
            sequenceDate = today;
            lastSequence = 0;
        }
        return ++lastSequence;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getTokenPrefix() { return tokenPrefix; }
    public boolean isOpen() { return open; }
    public int getDefaultServiceMinutes() { return defaultServiceMinutes; }
    public Instant getCreatedAt() { return createdAt; }
}
