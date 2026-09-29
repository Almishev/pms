package com.hotel.pms.backup;

import java.time.LocalDateTime;

public final class BackupSchedule {
    public static final int HOUR = 23;
    public static final int MINUTE = 35;

    private BackupSchedule() {
    }

    public static LocalDateTime dueSlot(LocalDateTime now) {
        LocalDateTime today = now.toLocalDate().atTime(HOUR, MINUTE);
        if (now.isBefore(today)) {
            return today.minusDays(1);
        }
        return today;
    }

    public static boolean isDue(LocalDateTime lastSuccess, LocalDateTime now) {
        LocalDateTime slot = dueSlot(now);
        return lastSuccess == null || lastSuccess.isBefore(slot);
    }
}
