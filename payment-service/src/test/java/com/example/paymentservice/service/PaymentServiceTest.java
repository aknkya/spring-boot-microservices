package com.example.paymentservice.service;

import com.example.paymentservice.dto.NotificationRequest;
import com.example.paymentservice.dto.NotificationResponse;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    private PaymentService paymentService;

    private final String notificationServiceUrl = "http://notification-service:8082";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, restTemplate, notificationServiceUrl);
    }

    @Test
    @DisplayName("processPayment: Valid positive amount results in SUCCESS and triggers notification")
    void testProcessPayment_Success() {
        PaymentRequest request = new PaymentRequest(10L, "John Doe", new BigDecimal("150.00"));
        Payment mockPayment = new Payment(10L, "John Doe", new BigDecimal("150.00"), PaymentStatus.SUCCESS, "TXN-ABC", "Approved");
        mockPayment.setId(1L);

        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        NotificationResponse notifResponse = new NotificationResponse(50L, "SENT", "Sent", LocalDateTime.now());
        when(restTemplate.postForEntity(eq("http://notification-service:8082/api/notifications"), any(NotificationRequest.class), eq(NotificationResponse.class)))
                .thenReturn(new ResponseEntity<>(notifResponse, HttpStatus.CREATED));

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(NotificationResponse.class));
    }

    @Test
    @DisplayName("processPayment: Zero or negative amount results in FAILED and does NOT trigger notification")
    void testProcessPayment_InvalidAmount() {
        PaymentRequest request = new PaymentRequest(11L, "Jane Doe", BigDecimal.ZERO);
        Payment mockPayment = new Payment(11L, "Jane Doe", BigDecimal.ZERO, PaymentStatus.FAILED, null, "Failed");
        mockPayment.setId(2L);

        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("FAILED");
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @DisplayName("processPayment: Notification service failure does not fail the payment")
    void testProcessPayment_NotificationFailure_PaymentStillSucceeds() {
        PaymentRequest request = new PaymentRequest(12L, "Bob", new BigDecimal("50.00"));
        Payment mockPayment = new Payment(12L, "Bob", new BigDecimal("50.00"), PaymentStatus.SUCCESS, "TXN-XYZ", "Approved");
        mockPayment.setId(3L);

        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        when(restTemplate.postForEntity(anyString(), any(), eq(NotificationResponse.class)))
                .thenThrow(new RestClientException("Notification service timeout"));

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getPaymentByOrderId: returns mapped payment when found")
    void testGetPaymentByOrderId_Found() {
        Payment payment = new Payment(5L, "Alice", new BigDecimal("20.00"), PaymentStatus.SUCCESS, "TXN-1", "OK");
        payment.setId(9L);
        when(paymentRepository.findByOrderId(5L)).thenReturn(Optional.of(payment));

        Optional<PaymentResponse> result = paymentService.getPaymentByOrderId(5L);

        assertThat(result).isPresent();
        assertThat(result.get().orderId()).isEqualTo(5L);
        assertThat(result.get().status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getAllPayments: returns all recorded payments")
    void testGetAllPayments() {
        Payment p1 = new Payment(1L, "A", BigDecimal.ONE, PaymentStatus.SUCCESS, "T1", "OK");
        when(paymentRepository.findAll()).thenReturn(List.of(p1));

        List<PaymentResponse> list = paymentService.getAllPayments();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).status()).isEqualTo("SUCCESS");
    }
}
