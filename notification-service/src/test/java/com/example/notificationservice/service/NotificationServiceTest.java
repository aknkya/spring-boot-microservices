package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.NotificationStatus;
import com.example.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    @DisplayName("sendNotification: Successfully saves and marks notification as SENT")
    void testSendNotification_Success() {
        NotificationRequest request = new NotificationRequest("John Doe", 100L, "Order confirmed", "PAYMENT_SUCCESS");
        Notification mockNotification = new Notification("John Doe", 100L, "Order confirmed", "PAYMENT_SUCCESS", NotificationStatus.SENT);
        mockNotification.setId(1L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.recipient()).isEqualTo("John Doe");
        assertThat(response.status()).isEqualTo("SENT");
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("sendNotification: Falls back to 'INFO' when type is null")
    void testSendNotification_DefaultType() {
        NotificationRequest request = new NotificationRequest("Alice", 101L, "System message", null);
        Notification mockNotification = new Notification("Alice", 101L, "System message", "INFO", NotificationStatus.SENT);
        mockNotification.setId(2L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SENT");
    }

    @Test
    @DisplayName("getNotificationsByOrderId: Returns list of notifications for order")
    void testGetNotificationsByOrderId() {
        Notification n = new Notification("Bob", 200L, "Msg", "INFO", NotificationStatus.SENT);
        n.setId(5L);
        when(notificationRepository.findByOrderId(200L)).thenReturn(List.of(n));

        List<NotificationResponse> list = notificationService.getNotificationsByOrderId(200L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).orderId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("getAllNotifications: Returns all notifications")
    void testGetAllNotifications() {
        Notification n1 = new Notification("A", 1L, "M1", "T1", NotificationStatus.SENT);
        Notification n2 = new Notification("B", 2L, "M2", "T2", NotificationStatus.SENT);
        when(notificationRepository.findAll()).thenReturn(List.of(n1, n2));

        List<NotificationResponse> list = notificationService.getAllNotifications();

        assertThat(list).hasSize(2);
    }
}
