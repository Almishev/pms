package com.hotel.pms.repository;

import com.hotel.pms.model.entity.FiscalReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FiscalReceiptRepository extends JpaRepository<FiscalReceipt, Long> {
    java.util.List<FiscalReceipt> findByPaymentId(Long paymentId);
}

