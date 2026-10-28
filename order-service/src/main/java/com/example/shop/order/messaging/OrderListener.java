package com.example.shop.order.messaging;

import com.example.shop.common.events.EventCodec;
import com.example.shop.common.events.Events.InventoryRejected;
import com.example.shop.common.events.Events.PaymentCompleted;
import com.example.shop.common.events.Events.PaymentFailed;
import com.example.shop.common.events.Topics;
import com.example.shop.order.OrderWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
class OrderListener {
    private final OrderWriter writer;
    private final EventCodec codec;
    private final Duration pendingTimeout;

    OrderListener(OrderWriter writer, EventCodec codec,
                  @Value("${orders.pending-timeout:PT15M}") Duration pendingTimeout) {
        this.writer = writer;
        this.codec = codec;
        this.pendingTimeout = pendingTimeout;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "order-service")
    void onPaymentCompleted(String payload) {
        writer.onPaymentCompleted(codec.read(payload, PaymentCompleted.class));
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "order-service")
    void onPaymentFailed(String payload) {
        writer.onPaymentFailed(codec.read(payload, PaymentFailed.class));
    }

    @KafkaListener(topics = Topics.INVENTORY_REJECTED, groupId = "order-service")
    void onInventoryRejected(String payload) {
        writer.onInventoryRejected(codec.read(payload, InventoryRejected.class));
    }

    @Scheduled(fixedDelayString = "${orders.timeout-check-ms:60000}")
    void cancelStaleOrders() {
        writer.cancelStale(Instant.now().minus(pendingTimeout));
    }
}
