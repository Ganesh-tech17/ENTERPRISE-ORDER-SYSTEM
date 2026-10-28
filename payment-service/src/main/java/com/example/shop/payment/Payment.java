package com.example.shop.payment;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payments(id, order_id, status) from the spec, plus amount/reason/timestamp.
 * The UNIQUE constraint on order_id is what makes payment processing idempotent: one payment per order, ever.
 */
@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(name = "uk_payments_order", columnNames = "order_id"))
public class Payment {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {}

    public Payment(UUID orderId, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public void complete() { this.status = PaymentStatus.COMPLETED; }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
