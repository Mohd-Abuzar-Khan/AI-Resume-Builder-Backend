package com.resumade.auth.controller;

import com.razorpay.RazorpayException;
import com.resumade.auth.dto.RazorpayOrderRequest;
import com.resumade.auth.dto.RazorpayOrderResponse;
import com.resumade.auth.entity.PaymentRecord;
import com.resumade.auth.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@RequestParam Integer userId, @RequestBody RazorpayOrderRequest request) throws RazorpayException {
        return ResponseEntity.ok(paymentService.createOrder(userId, request.getAmount()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> payload) {
        String orderId = payload.get("razorpay_order_id");
        String paymentId = payload.get("razorpay_payment_id");
        String signature = payload.get("razorpay_signature");

        paymentService.verifyPayment(orderId, paymentId, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<PaymentRecord>> getPaymentHistory(@PathVariable Integer userId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(userId));
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload) {
        // Simple webhook placeholder
        return ResponseEntity.ok().build();
    }
}
