package com.example.paymentservice.service;

import com.example.paymentservice.dto.NotificationRequest;
import com.example.paymentservice.dto.NotificationResponse;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          RestTemplate restTemplate,
                          @Value("${services.notification.url}") String notificationServiceUrl) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for Order ID: {}, Customer: {}, Amount: {}",
                request.orderId(), request.customerName(), request.amount());

        boolean isApproved = request.amount() != null && request.amount().compareTo(BigDecimal.ZERO) > 0;
        PaymentStatus status = isApproved ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        String transactionId = isApproved ? "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase() : null;
        String message = isApproved ? "Payment of $" + request.amount() + " processed successfully." : "Payment failed: Invalid amount.";

        // Step 1: Save Payment Record
        Payment payment = new Payment(
                request.orderId(),
                request.customerName(),
                request.amount(),
                status,
                transactionId,
                message
        );
        payment = paymentRepository.save(payment);
        log.info("Payment record created with ID: {}, Status: {}, Transaction ID: {}",
                payment.getId(), payment.getStatus(), payment.getTransactionId());

        // Step 2: If Payment is SUCCESS, trigger Notification Service via HTTP
        if (status == PaymentStatus.SUCCESS) {
            triggerNotification(payment);
        }

        return PaymentResponse.fromEntity(payment);
    }

    private void triggerNotification(Payment payment) {
        String notificationEndpoint = notificationServiceUrl + "/api/notifications";
        NotificationRequest notificationRequest = new NotificationRequest(
                payment.getCustomerName(),
                payment.getOrderId(),
                "Payment of $" + payment.getAmount() + " confirmed. Order #" + payment.getOrderId() + " is being prepared.",
                "PAYMENT_SUCCESS"
        );

        try {
            log.info("Sending notification for Order ID: {} to {}", payment.getOrderId(), notificationEndpoint);
            ResponseEntity<NotificationResponse> response = restTemplate.postForEntity(
                    notificationEndpoint,
                    notificationRequest,
                    NotificationResponse.class
            );

            NotificationResponse body = response.getBody();
            if (body != null) {
                log.info("Notification successfully sent! Notification ID: {}, Status: {}",
                        body.notificationId(), body.status());
            }
        } catch (Exception ex) {
            log.error("Failed to send notification for Order ID: {}. Error: {}",
                    payment.getOrderId(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResponse> getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResponse> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(PaymentResponse::fromEntity);
    }
}
