package com.hotel.pms.service;

import com.hotel.pms.model.enums.BookingStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void checkedInGuestKeepsTheRoomThroughToday() {
        LocalDate today = LocalDate.of(2026, 9, 30);
        LocalDate overdueCheckout = LocalDate.of(2026, 9, 28);

        LocalDate occupiedUntil = StayPeriod.occupiedUntil(BookingStatus.CHECKED_IN, overdueCheckout, today);

        assertEquals(today.plusDays(1), occupiedUntil);
        assertTrue(StayPeriod.overlaps(
                LocalDate.of(2026, 9, 25), occupiedUntil, today, today.plusDays(1)));
    }
}
