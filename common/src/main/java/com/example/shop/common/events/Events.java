package com.example.shop.common.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Event payloads exchanged over Kafka. One topic per event type, keyed by orderId. */
public final class Events {
    private Events() {}

    public record Item(String sku, int quantity) {}

    public record OrderCreated(UUID orderId, Long userId, BigDecimal total, List<Item> items) {}

    public record OrderCancelled(UUID orderId, String reason) {}

    public record InventoryReserved(UUID orderId, BigDecimal total) {}

    public record InventoryRejected(UUID orderId, String reason) {}

    public record PaymentCompleted(UUID orderId, UUID paymentId) {}

    public record PaymentFailed(UUID orderId, String reason) {}
}
