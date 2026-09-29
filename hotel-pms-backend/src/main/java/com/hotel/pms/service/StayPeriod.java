package com.hotel.pms.service;

import java.time.LocalDate;

/**
 * A stay occupies the half-open interval [checkIn, checkOut).
 * The checkout day is free for the next guest.
 */
public final class StayPeriod {
    private StayPeriod() {
    }

    public static boolean overlaps(LocalDate checkIn, LocalDate checkOut,
                                   LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return checkIn.isBefore(otherCheckOut) && checkOut.isAfter(otherCheckIn);
    }
}
