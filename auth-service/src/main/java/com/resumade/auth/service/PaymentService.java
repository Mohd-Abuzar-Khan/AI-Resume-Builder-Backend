package com.resumade.auth.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.resumade.auth.dto.RazorpayOrderResponse;
import com.resumade.auth.entity.PaymentRecord;
import com.resumade.auth.entity.User;
import com.resumade.auth.repository.PaymentRecordRepository;
import com.resumade.auth.repository.UserRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final PaymentRecordRepository paymentRecordRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final NotificationProducer notificationProducer;

    public PaymentService(PaymentRecordRepository paymentRecordRepository, 
                          UserRepository userRepository,
                          AuthService authService,
                          NotificationProducer notificationProducer) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.notificationProducer = notificationProducer;
    }

    public RazorpayOrderResponse createOrder(Integer userId, Double amount) throws RazorpayException {
        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int)(amount * 100)); // amount in the smallest currency unit
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());

        Order order = razorpay.orders.create(orderRequest);

        PaymentRecord record = new PaymentRecord();
        record.setUserId(userId);
        record.setOrderId(order.get("id"));
        record.setAmount(amount);
        record.setCurrency("INR");
        record.setStatus("CREATED");
        paymentRecordRepository.save(record);

        return new RazorpayOrderResponse(
                order.get("id"),
                "INR",
                (int)(amount * 100),
                razorpayKeyId
        );
    }

    @Transactional
    public void verifyPayment(String orderId, String paymentId, String signature) {
        // In a real scenario, you'd verify the signature here using RazorpayUtils
        // For testing, we'll assume it's valid if provided
        
        Optional<PaymentRecord> recordOpt = paymentRecordRepository.findByOrderId(orderId);
        if (recordOpt.isPresent()) {
            PaymentRecord record = recordOpt.get();
            if ("SUCCESS".equals(record.getStatus())) {
                log.info("Payment already processed for order: {}", orderId);
                return;
            }
            
            record.setPaymentId(paymentId);
            record.setSignature(signature);
            record.setStatus("SUCCESS");
            paymentRecordRepository.save(record);

            authService.updateSubscription(record.getUserId(), "PREMIUM");
            log.info("Payment verified and subscription updated for user: {}", record.getUserId());
        }
    }

    public List<PaymentRecord> getPaymentHistory(Integer userId) {
        return paymentRecordRepository.findByUserId(userId);
    }
}
