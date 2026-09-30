package com.example.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationRequest(
        @NotBlank(message = "Recipient is required")
        String recipient,

        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotBlank(message = "Message is required")
        String message,

        String type
) {
}
