package com.resumade.auth.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.resumade.auth.dto.RazorpayOrderResponse;
import com.resumade.auth.entity.PaymentRecord;
import com.resumade.auth.repository.PaymentRecordRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private PaymentService paymentService;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private AuthService authService;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private com.resumade.auth.repository.UserRepository userRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private Order order;

    @BeforeEach
    void setUp() {
        paymentService = spy(new PaymentService(paymentRecordRepository, userRepository, authService, notificationProducer));
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "rzp_secret");
    }

    @Test
    void createOrder_shouldPersistRecordAndReturnResponse() throws com.razorpay.RazorpayException {
        doReturn(razorpayClient).when(paymentService).createRazorpayClient();
        ReflectionTestUtils.setField(razorpayClient, "orders", orderClient);
        when(orderClient.create(any(org.json.JSONObject.class))).thenReturn(order);
        when(order.get("id")).thenReturn("order_123");

        RazorpayOrderResponse response = paymentService.createOrder(1, 499.0);

        assertEquals("order_123", response.getOrderId());
        assertEquals("INR", response.getCurrency());
        assertEquals(49900, response.getAmount());
        assertEquals("rzp_key", response.getKey());
        verify(paymentRecordRepository).save(any(PaymentRecord.class));
    }

    @Test
    void verifyPayment_updatesSubscriptionWhenValid() {
        String orderId = "order_123";
        PaymentRecord record = new PaymentRecord();
        record.setUserId(1);
        record.setOrderId(orderId);
        record.setStatus("CREATED");

        when(paymentRecordRepository.findByOrderId(orderId)).thenReturn(Optional.of(record));

        paymentService.verifyPayment(orderId, "pay_123", "sig_123");

        verify(authService).updateSubscription(1, "PREMIUM");
        verify(paymentRecordRepository).save(any(PaymentRecord.class));
    }

    @Test
    void verifyPayment_ignoresDuplicate() {
        String orderId = "order_123";
        PaymentRecord record = new PaymentRecord();
        record.setUserId(1);
        record.setOrderId(orderId);
        record.setStatus("SUCCESS");

        when(paymentRecordRepository.findByOrderId(orderId)).thenReturn(Optional.of(record));

        paymentService.verifyPayment(orderId, "pay_123", "sig_123");

        verify(authService, never()).updateSubscription(anyInt(), anyString());
    }

    @Test
    void verifyPayment_shouldDoNothingWhenRecordMissing() {
        when(paymentRecordRepository.findByOrderId("missing")).thenReturn(Optional.empty());

        paymentService.verifyPayment("missing", "pay", "sig");

        verify(authService, never()).updateSubscription(anyInt(), anyString());
        verify(paymentRecordRepository, never()).save(any(PaymentRecord.class));
    }

    @Test
    void getPaymentHistory_shouldReturnList() {
        List<PaymentRecord> records = List.of(new PaymentRecord());
        when(paymentRecordRepository.findByUserId(1)).thenReturn(records);

        List<PaymentRecord> result = paymentService.getPaymentHistory(1);

        assertEquals(1, result.size());
    }
}
