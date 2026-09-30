package com.hotel.pms.model.dto;

import lombok.Data;

@Data
public class OpenRoomDto {
    private Long bookingId;
    private String roomNumber;
    private String guestName;
}
