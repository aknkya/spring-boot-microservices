package com.example.orderservice.dto;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String customerName,
        String product,
        Integer quantity,
        BigDecimal price,
        OrderStatus status,
        String paymentTransactionId,
        String notes,
        LocalDateTime createdAt
) {
    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getProduct(),
                order.getQuantity(),
                order.getPrice(),
                order.getStatus(),
                order.getPaymentTransactionId(),
                order.getNotes(),
                order.getCreatedAt()
        );
    }
}
