package com.example.orderservice.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        Long orderId,
        String customerName,
        BigDecimal amount
) {
}
