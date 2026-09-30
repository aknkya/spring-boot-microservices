package com.example.orderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        String status,
        String transactionId,
        BigDecimal amount,
        String message,
        LocalDateTime createdAt
) {
}
