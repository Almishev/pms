package com.hotel.pms.model.entity;

import com.hotel.pms.model.enums.FiscalReceiptStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fiscal_receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiscalReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "fiscal_date")
    private LocalDateTime fiscalDate;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FiscalReceiptStatus status;
}

