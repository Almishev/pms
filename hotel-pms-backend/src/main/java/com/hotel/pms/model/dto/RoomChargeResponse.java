package com.hotel.pms.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomChargeResponse {
    private Long id;
    private Long bookingId;
    private String billId;
    private String roomNumber;
    private String guestName;
    private BigDecimal amount;
    private BigDecimal reversedAmount;
    private BigDecimal activeAmount;
    private BigDecimal restaurantCharge;
    private boolean alreadyPosted;
}
