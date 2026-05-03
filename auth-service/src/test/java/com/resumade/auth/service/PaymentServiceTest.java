package com.resumade.auth.service;

import com.resumade.auth.entity.PaymentRecord;
import com.resumade.auth.repository.PaymentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(paymentRecordRepository, userRepository, authService, notificationProducer);
    }

    @Test
    void verifyPayment_UpdatesSubscription_WhenValid() {
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
    void verifyPayment_IgnoresDuplicate() {
        String orderId = "order_123";
        PaymentRecord record = new PaymentRecord();
        record.setUserId(1);
        record.setOrderId(orderId);
        record.setStatus("SUCCESS");

        when(paymentRecordRepository.findByOrderId(orderId)).thenReturn(Optional.of(record));

        paymentService.verifyPayment(orderId, "pay_123", "sig_123");

        verify(authService, never()).updateSubscription(anyInt(), anyString());
    }
}
