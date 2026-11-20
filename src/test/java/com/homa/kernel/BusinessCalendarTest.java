package com.homa.kernel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessCalendarTest {

    private static Instant kualaLumpur(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(ZoneId.of("Asia/Kuala_Lumpur")).toInstant();
    }

    @Test
    @DisplayName("The business calendar runs on Kuala Lumpur time.")
    void calendarZoneIsKualaLumpur() {
        assertThat(BusinessCalendar.ZONE).isEqualTo(ZoneId.of("Asia/Kuala_Lumpur"));
    }

    @Test
    @DisplayName("00:30 in Kuala Lumpur on 1 December is still 30 November in UTC, but the business date is 1 December.")
    void dateOfAnInstantIsItsKualaLumpurDateNotItsUtcDate() {
        Instant justAfterMidnight = kualaLumpur(2026, 12, 1, 0, 30);

        assertThat(BusinessCalendar.dateOf(justAfterMidnight)).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(BusinessCalendar.dateOf(kualaLumpur(2026, 11, 30, 23, 59))).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    @DisplayName("90 days after 00:30 on 1 December is 00:30 on 1 March.")
    void plusDaysKeepsTheKualaLumpurWallClockTime() {
        Instant start = kualaLumpur(2026, 12, 1, 0, 30);

        assertThat(BusinessCalendar.plusDays(start, 90)).isEqualTo(kualaLumpur(2027, 3, 1, 0, 30));
        assertThat(BusinessCalendar.plusDays(start, -90)).isEqualTo(kualaLumpur(2026, 9, 2, 0, 30));
    }

    @Test
    @DisplayName("6 months after 31 August is the last day of February.")
    void plusMonthsClampsToTheLastDayOfAShorterMonth() {
        assertThat(BusinessCalendar.plusMonths(kualaLumpur(2026, 8, 31, 9, 0), 6))
                .isEqualTo(kualaLumpur(2027, 2, 28, 9, 0));
        assertThat(BusinessCalendar.plusMonths(kualaLumpur(2026, 12, 5, 9, 0), -6))
                .isEqualTo(kualaLumpur(2026, 6, 5, 9, 0));
    }
}
