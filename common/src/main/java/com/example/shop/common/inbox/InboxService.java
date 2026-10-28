package com.example.shop.common.inbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Consumer-side de-duplication ("inbox" pattern). Runs inside the consumer's DB transaction. */
@Service
public class InboxService {
    private final ProcessedEventRepository repository;

    public InboxService(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    /** @return true the first time an event id is seen, false for a redelivery. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstTime(String eventId) {
        if (repository.existsById(eventId)) {
            return false;
        }
        repository.save(new ProcessedEvent(eventId));
        return true;
    }
}
