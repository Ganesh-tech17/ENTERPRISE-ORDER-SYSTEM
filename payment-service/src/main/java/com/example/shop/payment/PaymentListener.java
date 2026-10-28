package com.example.shop.payment;

import com.example.shop.common.events.EventCodec;
import com.example.shop.common.events.Events.InventoryReserved;
import com.example.shop.common.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class PaymentListener {
    private final PaymentService service;
    private final EventCodec codec;

    PaymentListener(PaymentService service, EventCodec codec) {
        this.service = service;
        this.codec = codec;
    }

    /** Saga step 2: stock is reserved -> charge the customer. Redelivery is harmless (idempotent). */
    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = "payment-service")
    void onInventoryReserved(String payload) {
        InventoryReserved event = codec.read(payload, InventoryReserved.class);
        service.process(event.orderId(), event.total());
    }
}
