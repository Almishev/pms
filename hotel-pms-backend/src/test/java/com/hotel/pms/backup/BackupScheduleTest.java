package com.hotel.pms.backup;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupScheduleTest {
    @Test
    void beforeTheNightlySlotTheDueBackupIsYesterday() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 10, 0);
        assertEquals(LocalDateTime.of(2026, 9, 28, 23, 35), BackupSchedule.dueSlot(now));
    }

    @Test
    void afterTheNightlySlotTheDueBackupIsToday() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 23, 35);
        assertEquals(LocalDateTime.of(2026, 9, 29, 23, 35), BackupSchedule.dueSlot(now));
    }

    @Test
    void aMissedSlotRunsOnTheNextStart() {
        LocalDateTime last = LocalDateTime.of(2026, 9, 28, 10, 0);
        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 8, 0);
        assertTrue(BackupSchedule.isDue(last, now));
    }

    @Test
    void aFinishedSlotDoesNotRunAgain() {
        LocalDateTime last = LocalDateTime.of(2026, 9, 28, 23, 40);
        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 9, 0);
        assertFalse(BackupSchedule.isDue(last, now));
        assertTrue(BackupSchedule.isDue(null, now));
    }
}
