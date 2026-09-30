package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/orders: Creates order and returns 201 Created")
    void testCreateOrder_Success() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("Alice", "Keyboard", 1, new BigDecimal("75.00"));
        OrderResponse response = new OrderResponse(
                1L,
                "Alice",
                "Keyboard",
                1,
                new BigDecimal("75.00"),
                OrderStatus.COMPLETED,
                "TXN-999",
                "Success",
                LocalDateTime.now()
        );

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/orders/{id}: Returns 200 OK when found")
    void testGetOrderById_Found() throws Exception {
        OrderResponse response = new OrderResponse(
                5L,
                "Charlie",
                "Mouse",
                1,
                new BigDecimal("25.00"),
                OrderStatus.COMPLETED,
                "TXN-111",
                "Success",
                LocalDateTime.now()
        );

        when(orderService.getOrderById(5L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.product").value("Mouse"));
    }

    @Test
    @DisplayName("GET /api/orders/{id}: Returns 404 Not Found when absent")
    void testGetOrderById_NotFound() throws Exception {
        when(orderService.getOrderById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/orders: Returns 200 OK with list of orders")
    void testGetAllOrders() throws Exception {
        OrderResponse o1 = new OrderResponse(1L, "U1", "P1", 1, BigDecimal.TEN, OrderStatus.COMPLETED, "T1", "", LocalDateTime.now());
        when(orderService.getAllOrders()).thenReturn(List.of(o1));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerName").value("U1"));
    }
}
