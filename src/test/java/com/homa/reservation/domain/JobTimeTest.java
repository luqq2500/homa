package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.CHECK_IN_PERIOD;
import static com.homa.fixtures.HomaFixtures.JOB_BUFFER;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.JOB_TIME;
import static com.homa.fixtures.HomaFixtures.PROTECTION_WINDOW;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.sunday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.TimeInterval;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class JobTimeTest {

    private final JobTime standard = JobTime.of(JOB_START, STANDARD_HOURS);

    @Test
    @Tag("JOB-1")
    @DisplayName("The team arrives at Aisyah's home at 9:52 and starts at 10:00.")
    void teamArrivesDuringTheCheckInPeriodBeforeTheJobStarts() {
        assertThat(standard.checkInPeriod()).isEqualTo(CHECK_IN_PERIOD);
        assertThat(standard.phaseAt(saturday(9, 52))).isEqualTo(ReservationPhase.CHECK_IN);
        assertThat(standard.phaseAt(JOB_START)).isEqualTo(ReservationPhase.IN_JOB);
    }

    @Test
    @Tag("JOB-1")
    @DisplayName("The phase changes exactly at 9:45, 10:00, 14:00 and Sunday 14:00.")
    void phaseChangesExactlyAtEachTimeEdge() {
        assertThat(standard.phaseAt(saturday(9, 44))).isEqualTo(ReservationPhase.BEFORE_CHECK_IN);
        assertThat(standard.phaseAt(saturday(9, 45))).isEqualTo(ReservationPhase.CHECK_IN);
        assertThat(standard.phaseAt(saturday(9, 59))).isEqualTo(ReservationPhase.CHECK_IN);
        assertThat(standard.phaseAt(saturday(10, 0))).isEqualTo(ReservationPhase.IN_JOB);
        assertThat(standard.phaseAt(saturday(13, 59))).isEqualTo(ReservationPhase.IN_JOB);
        assertThat(standard.phaseAt(saturday(14, 0))).isEqualTo(ReservationPhase.POST_JOB);
        assertThat(standard.phaseAt(sunday(13, 59))).isEqualTo(ReservationPhase.POST_JOB);
        assertThat(standard.phaseAt(sunday(14, 0))).isEqualTo(ReservationPhase.CLOSED);
    }

    @Test
    @DisplayName("The standard job runs Saturday 10:00–14:00, protected from 9:45 until Sunday 14:00.")
    void jobTimeEndsAfterItsHoursAndIsProtectedUntilADayLater() {
        assertThat(standard.end()).isEqualTo(JOB_END);
        assertThat(standard.interval()).isEqualTo(JOB_TIME);
        assertThat(standard.protectionWindow()).isEqualTo(PROTECTION_WINDOW);
    }

    @Test
    @Tag("RES-4")
    @DisplayName("Requested on Saturday 7:00 for 10:00, the Notice is 2h 45m, up to the Check-in Period at 9:45.")
    void noticeRunsUntilTheCheckInPeriodStarts() {
        assertThat(standard.noticeFrom(saturday(7, 0))).isEqualTo(Duration.ofMinutes(165));
    }

    @Test
    @Tag("RES-9")
    @DisplayName("Aisyah's request for Saturday 10:00–14:00 blocks Kemas Home from 9:45 to 14:30.")
    void calendarBlockRunsFromCheckInToJobEndPlusBuffer() {
        assertThat(standard.calendarBlock(JOB_BUFFER)).isEqualTo(new TimeInterval(saturday(9, 45), saturday(14, 30)));
    }

    @Test
    @Tag("JOB-5")
    @DisplayName("Kemas Home forgets. It's reminded at 14:00 and 16:00.")
    void completionRemindersComeAtJobEndAndTwoHoursLater() {
        assertThat(standard.completionReminders()).containsExactly(saturday(14, 0), saturday(16, 0));
    }
}
