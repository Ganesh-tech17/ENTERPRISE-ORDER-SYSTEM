package com.example.shop.common.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(length = 200)
    private String id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(String id) {
        this.id = id;
        this.processedAt = Instant.now();
    }

    public String getId() { return id; }
}
