package com.example.shop.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A message that must be sent to Kafka. Written in the SAME DB transaction as the business change. */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean published;

    protected OutboxEvent() {}

    public OutboxEvent(String topic, String messageKey, String payload) {
        this.id = UUID.randomUUID();
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.published = false;
    }

    public UUID getId() { return id; }
    public String getTopic() { return topic; }
    public String getMessageKey() { return messageKey; }
    public String getPayload() { return payload; }
    public boolean isPublished() { return published; }
    public void markPublished() { this.published = true; }
}
