package com.hotel.pms.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingDto {
    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Guest ID is required")
    private Long guestId;

    @NotNull(message = "Check-in date is required")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOutDate;

    // Optional: custom price per night (if not provided, uses room type base price)
    private BigDecimal customPricePerNight;
}

