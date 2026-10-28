package com.example.shop.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
class FallbackController {

    @RequestMapping("/fallback")
    Mono<ResponseEntity<Map<String, String>>> fallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "10")
                .body(Map.of("error", "SERVICE_UNAVAILABLE",
                        "message", "The service is temporarily unavailable. Please retry shortly.")));
    }
}
