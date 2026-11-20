package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.domain.ReservationFixtures.SATURDAY;
import static com.homa.reservation.domain.ReservationFixtures.anotherCustomerBooking;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class AvailableStartTimesTest {

    private final Marketplace market = Marketplace.withKemasHome();

    @Test
    @Tag("RES-3")
    @DisplayName("Kemas Home has a job Saturday 8:00–10:00 elsewhere. With its 30-minute buffer, the earliest start Aisyah can choose is 10:45, with the Check-in Period from 10:30.")
    void earliestStartAfterAnotherJobLeavesTheBufferAndCheckIn() {
        market.book(anotherCustomerBooking(saturday(8, 0), Duration.ofHours(2), 2), monday(19, 0), monday(19, 1));

        List<Instant> starts = startsOn(REQUESTED_AT);

        assertThat(starts).first().isEqualTo(saturday(10, 45));
        assertThat(starts).doesNotContain(saturday(10, 30)).last().isEqualTo(saturday(14, 0));
    }

    @Test
    @Tag("RES-4")
    @DisplayName("It's Saturday 7:00, Kemas Home has no other jobs that morning, and its notice is 2 hours. The earliest job start Aisyah can pick is 9:15.")
    void earliestStartLeavesTheMinimumNotice() {
        assertThat(startsOn(saturday(7, 0))).first().isEqualTo(saturday(9, 15));
    }

    @Test
    @Tag("RES-5")
    @DisplayName("Start times come in 15-minute steps from opening.")
    void startTimesComeInQuarterHourStepsFromOpening() {
        assertThat(startsOn(REQUESTED_AT)).startsWith(saturday(8, 0), saturday(8, 15), saturday(8, 30)).hasSize(25);
    }

    @Test
    @Tag("RES-3")
    @DisplayName("Kemas Home doesn't work on Sundays, so no start time is offered.")
    void noStartTimeOnADayTheProviderDoesNotWork() {
        assertThat(AvailableStartTimes.on(SATURDAY.plusDays(1), standardBooking(), market.provider(),
                market.providerSchedule(), REQUESTED_AT)).isEmpty();
    }

    private List<Instant> startsOn(Instant now) {
        return AvailableStartTimes.on(SATURDAY, standardBooking(), market.provider(), market.providerSchedule(), now);
    }
}
