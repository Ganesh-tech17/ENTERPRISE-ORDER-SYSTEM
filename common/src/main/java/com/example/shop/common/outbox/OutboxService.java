package com.example.shop.common.outbox;

import com.example.shop.common.events.EventCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OutboxService {
    private final OutboxRepository repository;
    private final EventCodec codec;

    public OutboxService(OutboxRepository repository, EventCodec codec) {
        this.repository = repository;
        this.codec = codec;
    }

    /** MANDATORY: must be called inside the business transaction, never on its own. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, UUID key, Object event) {
        repository.save(new OutboxEvent(topic, key.toString(), codec.write(event)));
    }
}
