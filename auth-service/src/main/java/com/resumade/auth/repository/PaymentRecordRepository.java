package com.resumade.auth.repository;

import com.resumade.auth.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByUserId(Integer userId);
    Optional<PaymentRecord> findByOrderId(String orderId);
}
