package com.hotel.pms.service;

import com.hotel.pms.model.enums.BookingStatus;

import java.time.LocalDate;

/**
 * A stay occupies the half-open interval [checkIn, checkOut).
 * The checkout day is free for the next guest.
 * A guest who is still checked in keeps the room through today.
 */
public final class StayPeriod {
    private StayPeriod() {
    }

    public static boolean overlaps(LocalDate checkIn, LocalDate checkOut,
                                   LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return checkIn.isBefore(otherCheckOut) && checkOut.isAfter(otherCheckIn);
    }

    public static LocalDate occupiedUntil(BookingStatus status, LocalDate checkOut, LocalDate today) {
        if (status == BookingStatus.CHECKED_IN) {
            LocalDate throughToday = today.plusDays(1);
            if (checkOut.isBefore(throughToday)) {
                return throughToday;
            }
        }
        return checkOut;
    }
}
