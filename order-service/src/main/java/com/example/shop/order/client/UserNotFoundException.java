package com.example.shop.order.client;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User " + userId + " does not exist");
    }
}
