package com.resumade.auth.controller;

import com.resumade.auth.dto.RazorpayOrderResponse;
import com.resumade.auth.entity.PaymentRecord;
import com.resumade.auth.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void createOrder_shouldReturnOrder() throws Exception {
        com.resumade.auth.dto.RazorpayOrderRequest request = new com.resumade.auth.dto.RazorpayOrderRequest();
        request.setAmount(499.0);

        RazorpayOrderResponse mockResponse = new RazorpayOrderResponse("order_123", "INR", 49900, "rzp_key");
        when(paymentService.createOrder(1, 499.0)).thenReturn(mockResponse);

        ResponseEntity<RazorpayOrderResponse> response = paymentController.createOrder(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("order_123", response.getBody().getOrderId());
    }

    @Test
    void verifyPayment_shouldReturnOk() {
        Map<String, String> payload = new HashMap<>();
        payload.put("razorpay_order_id", "order_123");
        payload.put("razorpay_payment_id", "pay_456");
        payload.put("razorpay_signature", "sig_789");

        ResponseEntity<?> response = paymentController.verifyPayment(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentService).verifyPayment("order_123", "pay_456", "sig_789");
    }

    @Test
    void getPaymentHistory_shouldReturnList() {
        List<PaymentRecord> history = Arrays.asList(new PaymentRecord());
        when(paymentService.getPaymentHistory(1)).thenReturn(history);

        ResponseEntity<List<PaymentRecord>> response = paymentController.getPaymentHistory(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void handleWebhook_shouldReturnOk() {
        ResponseEntity<?> response = paymentController.handleWebhook("{}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
