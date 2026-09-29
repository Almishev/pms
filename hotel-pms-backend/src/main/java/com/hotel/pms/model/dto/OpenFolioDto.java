package com.hotel.pms.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OpenFolioDto {
    private Long roomId;
    private Long bookingId;
    private String guestName;
    private BigDecimal nightsTotal;
    private BigDecimal restaurantCharge;
    private BigDecimal total;
}
