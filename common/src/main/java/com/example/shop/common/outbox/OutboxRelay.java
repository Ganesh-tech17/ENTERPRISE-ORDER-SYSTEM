package com.example.shop.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox and ships events to Kafka. If Kafka is down the events simply stay in the table
 * and are retried on the next tick - orders are still accepted. Delivery is at-least-once, so every
 * consumer is idempotent.
 *
 * Production hardening: with several instances use SELECT ... FOR UPDATE SKIP LOCKED, or Debezium CDC.
 */
@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafka) {
        this.repository = repository;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${outbox.relay-delay-ms:1000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = repository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : batch) {
            try {
                kafka.send(event.getTopic(), event.getMessageKey(), event.getPayload()).get(5, TimeUnit.SECONDS);
                event.markPublished();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Stop at the first failure to preserve ordering; retry on the next tick.
                log.warn("Kafka unavailable ({}). {} event(s) remain in the outbox, will retry.",
                        e.getClass().getSimpleName(), batch.size());
                return;
            }
        }
    }
}
