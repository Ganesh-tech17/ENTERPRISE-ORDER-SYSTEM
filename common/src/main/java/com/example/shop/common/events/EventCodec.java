package com.example.shop.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class EventCodec {
    private final ObjectMapper mapper;

    public EventCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event", e);
        }
    }

    /** Throws IllegalArgumentException (treated as non-retryable -> straight to the DLT) on poison messages. */
    public <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Bad event payload for " + type.getSimpleName(), e);
        }
    }
}
