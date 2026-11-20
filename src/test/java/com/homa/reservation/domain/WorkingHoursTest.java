package com.homa.reservation.domain;

import static com.homa.reservation.domain.ReservationFixtures.SATURDAY;
import static com.homa.reservation.domain.ReservationFixtures.kemasWorkingHours;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class WorkingHoursTest {

    @Test
    @Tag("RES-3")
    @DisplayName("Kemas Home works Saturday 8:00–18:00, so a 4-hour job can start every 15 minutes from 8:00 to 14:00.")
    void startTimesRunFromOpeningUntilTheLastStartThatEndsByClosing() {
        List<Instant> starts = kemasWorkingHours().startsOn(SATURDAY, QuarterHourDuration.ofMinutes(240));

        assertThat(starts).hasSize(25).startsWith(saturday(8, 0), saturday(8, 15)).endsWith(saturday(14, 0));
    }
}
