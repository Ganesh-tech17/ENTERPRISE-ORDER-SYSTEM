package com.example.shop.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Orders(id, user_id, status, total) from the spec, plus line items, failure reason and audit fields. */
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version; // optimistic locking: two concurrent state changes cannot silently overwrite each other

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderLine> items = new ArrayList<>();

    protected Order() {}

    public Order(Long userId, List<OrderLine> items, BigDecimal total) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.items = new ArrayList<>(items);
        this.total = total;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
    }

    /** @return true if the transition happened (only PENDING orders can change state). */
    public boolean confirm() {
        if (status != OrderStatus.PENDING) return false;
        status = OrderStatus.CONFIRMED;
        return true;
    }

    public boolean cancel(String reason) {
        if (status != OrderStatus.PENDING) return false;
        status = OrderStatus.CANCELLED;
        failureReason = reason;
        return true;
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLine> getItems() { return items; }
}
