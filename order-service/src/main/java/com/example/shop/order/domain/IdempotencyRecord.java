package com.example.shop.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Maps (userId + Idempotency-Key) -> the order it created. The primary key is the arbiter of duplicates. */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {
    @Id
    @Column(name = "idem_key", length = 200)
    private String idemKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {}

    public IdempotencyRecord(String idemKey, String requestHash, UUID orderId) {
        this.idemKey = idemKey;
        this.requestHash = requestHash;
        this.orderId = orderId;
        this.createdAt = Instant.now();
    }

    public String getRequestHash() { return requestHash; }
    public UUID getOrderId() { return orderId; }
}
