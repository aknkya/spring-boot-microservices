package com.example.paymentservice.dto;

import com.example.paymentservice.entity.Payment;
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
    public static PaymentResponse fromEntity(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus().name(),
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getMessage(),
                payment.getCreatedAt()
        );
    }
}
