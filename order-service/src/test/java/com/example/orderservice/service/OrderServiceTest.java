package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.PaymentRequest;
import com.example.orderservice.dto.PaymentResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    private OrderService orderService;

    private final String paymentServiceUrl = "http://payment-service:8081";

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, restTemplate, paymentServiceUrl);
    }

    @Test
    @DisplayName("createOrder: Successful payment marks order as COMPLETED")
    void testCreateOrder_Success() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest("John Doe", "Laptop", 2, new BigDecimal("1200.00"));
        Order mockSavedOrder = new Order("John Doe", "Laptop", 2, new BigDecimal("2400.00"), OrderStatus.PENDING);
        mockSavedOrder.setId(100L);

        when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

        PaymentResponse mockPaymentResponse = new PaymentResponse(
                1L,
                100L,
                "SUCCESS",
                "TXN-123456",
                new BigDecimal("2400.00"),
                "Payment approved",
                LocalDateTime.now()
        );
        when(restTemplate.postForEntity(eq("http://payment-service:8081/api/payments"), any(PaymentRequest.class), eq(PaymentResponse.class)))
                .thenReturn(new ResponseEntity<>(mockPaymentResponse, HttpStatus.OK));

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(mockSavedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(mockSavedOrder.getPaymentTransactionId()).isEqualTo("TXN-123456");
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: Rejected payment marks order as FAILED")
    void testCreateOrder_PaymentRejected() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest("Jane Smith", "Phone", 1, new BigDecimal("800.00"));
        Order mockSavedOrder = new Order("Jane Smith", "Phone", 1, new BigDecimal("800.00"), OrderStatus.PENDING);
        mockSavedOrder.setId(101L);

        when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

        PaymentResponse mockPaymentResponse = new PaymentResponse(
                2L,
                101L,
                "FAILED",
                null,
                new BigDecimal("800.00"),
                "Insufficient funds",
                LocalDateTime.now()
        );
        when(restTemplate.postForEntity(eq("http://payment-service:8081/api/payments"), any(PaymentRequest.class), eq(PaymentResponse.class)))
                .thenReturn(new ResponseEntity<>(mockPaymentResponse, HttpStatus.BAD_REQUEST));

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(mockSavedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(mockSavedOrder.getNotes()).contains("Insufficient funds");
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: Network error calling Payment Service marks order as FAILED")
    void testCreateOrder_PaymentServiceDown() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest("Alice", "Headphones", 1, new BigDecimal("150.00"));
        Order mockSavedOrder = new Order("Alice", "Headphones", 1, new BigDecimal("150.00"), OrderStatus.PENDING);
        mockSavedOrder.setId(102L);

        when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

        when(restTemplate.postForEntity(anyString(), any(PaymentRequest.class), eq(PaymentResponse.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(mockSavedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(mockSavedOrder.getNotes()).contains("Connection refused");
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    @DisplayName("getOrderById: returns order when present")
    void testGetOrderById_Found() {
        Order mockOrder = new Order("Bob", "Monitor", 1, new BigDecimal("300.00"), OrderStatus.COMPLETED);
        mockOrder.setId(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(mockOrder));

        Optional<OrderResponse> result = orderService.getOrderById(10L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(10L);
        assertThat(result.get().customerName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("getOrderById: returns empty when not found")
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<OrderResponse> result = orderService.getOrderById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllOrders: returns all mapped orders")
    void testGetAllOrders() {
        Order o1 = new Order("User1", "Item1", 1, new BigDecimal("10.00"), OrderStatus.COMPLETED);
        o1.setId(1L);
        Order o2 = new Order("User2", "Item2", 2, new BigDecimal("20.00"), OrderStatus.COMPLETED);
        o2.setId(2L);

        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

        List<OrderResponse> orders = orderService.getAllOrders();

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).product()).isEqualTo("Item1");
        assertThat(orders.get(1).product()).isEqualTo("Item2");
    }
}
