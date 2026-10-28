package com.example.shop.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Synchronous call to the User service, protected by:
 *  - timeouts (never wait forever),
 *  - Retry with exponential backoff (200ms, 400ms, 800ms) for transient failures,
 *  - a Circuit Breaker that opens after repeated failures and fails fast instead of piling up threads.
 * See resilience4j.* in application.yml. Retry wraps the circuit breaker (Resilience4j default order).
 */
@Component
public class UserClient {

    private final RestClient rest;

    public UserClient(RestClient.Builder builder, @Value("${clients.user-service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(2000);
        this.rest = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Retry(name = "userService")
    @CircuitBreaker(name = "userService")
    public void assertUserExists(Long userId) {
        rest.get().uri("/users/{id}", userId)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new UserNotFoundException(userId);
                })
                .toBodilessEntity();
    }
}
