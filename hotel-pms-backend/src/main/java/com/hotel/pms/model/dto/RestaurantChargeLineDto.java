package com.hotel.pms.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RestaurantChargeLineDto {
    private Long id;
    private String externalBillId;
    private String tableName;
    private BigDecimal amount;
    private BigDecimal reversedAmount;
    private BigDecimal activeAmount;
    private String source;
    private LocalDateTime createdAt;
}
