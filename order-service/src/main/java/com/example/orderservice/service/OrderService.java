package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.PaymentRequest;
import com.example.orderservice.dto.PaymentResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
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

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public OrderService(OrderRepository orderRepository,
                        RestTemplate restTemplate,
                        @Value("${services.payment.url}") String paymentServiceUrl) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for customer: {}, product: {}, quantity: {}",
                request.customerName(), request.product(), request.quantity());

        BigDecimal totalPrice = request.price().multiply(BigDecimal.valueOf(request.quantity()));

        // Step 1: Save initial order with PENDING status
        Order order = new Order(
                request.customerName(),
                request.product(),
                request.quantity(),
                totalPrice,
                OrderStatus.PENDING
        );
        order = orderRepository.save(order);
        log.info("Order saved with ID: {} and status: PENDING", order.getId());

        // Step 2: HTTP call to Payment Service
        String paymentEndpoint = paymentServiceUrl + "/api/payments";
        PaymentRequest paymentRequest = new PaymentRequest(
                order.getId(),
                order.getCustomerName(),
                totalPrice
        );

        try {
            log.info("Calling Payment Service at: {} for Order ID: {}", paymentEndpoint, order.getId());
            ResponseEntity<PaymentResponse> paymentEntity = restTemplate.postForEntity(
                    paymentEndpoint,
                    paymentRequest,
                    PaymentResponse.class
            );

            PaymentResponse paymentResponse = paymentEntity.getBody();

            if (paymentResponse != null && "SUCCESS".equalsIgnoreCase(paymentResponse.status())) {
                log.info("Payment successful for Order ID: {}. Transaction ID: {}",
                        order.getId(), paymentResponse.transactionId());
                order.setStatus(OrderStatus.COMPLETED);
                order.setPaymentTransactionId(paymentResponse.transactionId());
                order.setNotes("Payment completed successfully");
            } else {
                String errorMsg = (paymentResponse != null) ? paymentResponse.message() : "Unknown payment error";
                log.warn("Payment rejected for Order ID: {}. Reason: {}", order.getId(), errorMsg);
                order.setStatus(OrderStatus.FAILED);
                order.setNotes("Payment rejected: " + errorMsg);
            }
        } catch (Exception ex) {
            log.error("Payment call failed for Order ID: {}. Error: {}", order.getId(), ex.getMessage());
            order.setStatus(OrderStatus.FAILED);
            order.setNotes("Payment call error: " + ex.getMessage());
        }

        // Step 3: Save final order state
        order = orderRepository.save(order);
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::fromEntity);
    }
}
