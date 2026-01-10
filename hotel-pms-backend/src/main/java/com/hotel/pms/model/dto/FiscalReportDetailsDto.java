package com.hotel.pms.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiscalReportDetailsDto {
    private String reportType; // Z_REPORT or X_REPORT
    private String reportNumber;
    private LocalDateTime fiscalDate;
    private LocalDate reportDate; // Date for which the report is generated
    
    // Summary information
    private BigDecimal totalAmount;
    private BigDecimal cashAmount;
    private BigDecimal cardAmount;
    private Integer totalPayments;
    private Integer cashPayments;
    private Integer cardPayments;
    
    // List of payments included in the report
    private List<PaymentSummary> payments;
    
    // Additional report data
    private Map<String, Object> additionalData;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private Long paymentId;
        private Long bookingId;
        private String guestName;
        private String roomNumber;
        private BigDecimal amount;
        private String paymentMethod;
        private LocalDateTime paymentDate;
    }
}

