package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.NotificationStatus;
import com.example.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("=================================================");
        log.info("📧 NOTIFICATION DISPATCHED");
        log.info("Recipient : {}", request.recipient());
        log.info("Order ID  : {}", request.orderId());
        log.info("Type      : {}", request.type());
        log.info("Message   : {}", request.message());
        log.info("=================================================");

        Notification notification = new Notification(
                request.recipient(),
                request.orderId(),
                request.message(),
                request.type() != null ? request.type() : "INFO",
                NotificationStatus.SENT
        );

        notification = notificationRepository.save(notification);
        log.info("Notification record saved in DB with ID: {}", notification.getId());

        return NotificationResponse.fromEntity(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByOrderId(Long orderId) {
        return notificationRepository.findByOrderId(orderId).stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }
}
