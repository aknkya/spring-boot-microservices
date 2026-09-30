package com.example.notificationservice.dto;

import com.example.notificationservice.entity.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        String recipient,
        Long orderId,
        String status,
        String message,
        LocalDateTime sentAt
) {
    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipient(),
                notification.getOrderId(),
                notification.getStatus().name(),
                notification.getMessage(),
                notification.getSentAt()
        );
    }
}
