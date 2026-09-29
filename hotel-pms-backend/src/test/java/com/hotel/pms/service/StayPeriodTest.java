package com.hotel.pms.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StayPeriodTest {

    @Test
    void checkoutDayDoesNotOverlapTheNextStay() {
        LocalDate existingCheckIn = LocalDate.of(2026, 9, 1);
        LocalDate existingCheckOut = LocalDate.of(2026, 9, 5);
        LocalDate nextCheckIn = LocalDate.of(2026, 9, 5);
        LocalDate nextCheckOut = LocalDate.of(2026, 9, 8);

        assertFalse(StayPeriod.overlaps(existingCheckIn, existingCheckOut, nextCheckIn, nextCheckOut));
    }

    @Test
    void sharedNightOverlaps() {
        LocalDate existingCheckIn = LocalDate.of(2026, 9, 1);
        LocalDate existingCheckOut = LocalDate.of(2026, 9, 5);
        LocalDate nextCheckIn = LocalDate.of(2026, 9, 4);
        LocalDate nextCheckOut = LocalDate.of(2026, 9, 8);

        assertTrue(StayPeriod.overlaps(existingCheckIn, existingCheckOut, nextCheckIn, nextCheckOut));
    }

    @Test
    void calendarDayIsOccupiedUntilCheckout() {
        LocalDate checkIn = LocalDate.of(2026, 9, 1);
        LocalDate checkOut = LocalDate.of(2026, 9, 5);

        assertTrue(StayPeriod.overlaps(checkIn, checkOut, LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 5)));
        assertFalse(StayPeriod.overlaps(checkIn, checkOut, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 6)));
    }
}
