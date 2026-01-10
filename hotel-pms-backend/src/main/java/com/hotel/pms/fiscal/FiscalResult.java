package com.hotel.pms.fiscal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiscalResult {
    private boolean success;
    private String receiptNumber;
    private LocalDateTime fiscalDate;
    private BigDecimal totalAmount;
    private String errorMessage;
}

