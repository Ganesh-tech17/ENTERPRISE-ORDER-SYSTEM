package com.example.shop.inventory;

import com.example.shop.common.events.EventCodec;
import com.example.shop.common.events.Events.OrderCancelled;
import com.example.shop.common.events.Events.OrderCreated;
import com.example.shop.common.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class InventoryListener {
    private final InventoryService service;
    private final EventCodec codec;

    InventoryListener(InventoryService service, EventCodec codec) {
        this.service = service;
        this.codec = codec;
    }

    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = "inventory-service")
    void onOrderCreated(String payload) {
        service.onOrderCreated(codec.read(payload, OrderCreated.class));
    }

    @KafkaListener(topics = Topics.ORDER_CANCELLED, groupId = "inventory-service")
    void onOrderCancelled(String payload) {
        service.onOrderCancelled(codec.read(payload, OrderCancelled.class));
    }
}
