package com.example.paymentservice.dto;

public record NotificationRequest(
        String recipient,
        Long orderId,
        String message,
        String type
) {
}
