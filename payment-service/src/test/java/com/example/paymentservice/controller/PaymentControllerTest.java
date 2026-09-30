package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.service.PaymentService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/payments: Process payment returns 200 OK")
    void testProcessPayment_Returns200() throws Exception {
        PaymentRequest request = new PaymentRequest(10L, "John", new BigDecimal("100.00"));
        PaymentResponse response = new PaymentResponse(
                1L,
                10L,
                "SUCCESS",
                "TXN-1234",
                new BigDecimal("100.00"),
                "Approved",
                LocalDateTime.now()
        );

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(10L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/payments/{id}: Found returns 200 OK")
    void testGetPaymentById_Found() throws Exception {
        PaymentResponse response = new PaymentResponse(
                2L,
                20L,
                "SUCCESS",
                "TXN-456",
                new BigDecimal("50.00"),
                "Approved",
                LocalDateTime.now()
        );
        when(paymentService.getPaymentById(2L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/payments/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(2L))
                .andExpect(jsonPath("$.orderId").value(20L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/payments/{id}: Not found returns 404")
    void testGetPaymentById_NotFound() throws Exception {
        when(paymentService.getPaymentById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound());
    }
}
