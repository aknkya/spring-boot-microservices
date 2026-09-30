package com.example.paymentservice.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        String status,
        String message,
        LocalDateTime sentAt
) {
}
