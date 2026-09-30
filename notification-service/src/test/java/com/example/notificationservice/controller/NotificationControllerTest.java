package com.example.notificationservice.controller;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/notifications: Sends notification and returns 201 Created")
    void testSendNotification_Created() throws Exception {
        NotificationRequest request = new NotificationRequest("Customer1", 55L, "Test message", "INFO");
        NotificationResponse response = new NotificationResponse(1L, "Customer1", 55L, "SENT", "Notification logged", LocalDateTime.now());

        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipient").value("Customer1"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("GET /api/notifications/order/{orderId}: Returns 200 with list")
    void testGetNotificationsByOrderId() throws Exception {
        NotificationResponse n1 = new NotificationResponse(1L, "C1", 55L, "SENT", "OK", LocalDateTime.now());
        when(notificationService.getNotificationsByOrderId(55L)).thenReturn(List.of(n1));

        mockMvc.perform(get("/api/notifications/order/55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(55L));
    }
}
