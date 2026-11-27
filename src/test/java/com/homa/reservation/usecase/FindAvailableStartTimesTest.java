package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.KITCHEN_CLEANING;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.usecase.dto.FindAvailableStartTimesRequest;
import com.homa.reservation.usecase.exception.CustomerNotFoundException;
import com.homa.reservation.usecase.exception.ProviderNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class FindAvailableStartTimesTest {

    private static final LocalDate SATURDAY = LocalDate.of(2026, 12, 5);

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private List<Instant> startsFor(String customerId, int minutes) {
        return bed.findAvailableStartTimes.find(new FindAvailableStartTimesRequest(customerId,
                KEMAS_HOME_PROVIDER_ID, SATURDAY, List.of(KITCHEN_CLEANING), minutes, 2)).startTimes();
    }

    @Test
    @Tag("RES-3")
    @DisplayName("A 4-hour job can start from 8:00 to 14:00 in 15-minute steps, ending by 18:00, not 17:00–21:00.")
    void startTimesFallWithinTheWorkingHours() {
        List<Instant> expected = IntStream.rangeClosed(0, 24)
                .mapToObj(step -> saturday(8, 0).plusSeconds(step * 15L * 60))
                .toList();

        assertThat(startsFor(AISYAH_CUSTOMER_ID, 240)).isEqualTo(expected);
    }

    @Test
    @Tag("RES-9")
    @DisplayName("Aisyah's request blocks Kemas Home on Saturday 9:45–14:30. No one can book it at 11:00; the next start is 14:45.")
    void timeBlockedByAnotherReservationIsNotOffered() {
        bed.requestStandard();
        bed.time.set(ReservationTestBed.friday(9, 0));

        List<Instant> starts = startsFor(MEI_CUSTOMER_ID, 120);

        assertThat(starts).first().isEqualTo(saturday(14, 45));
        assertThat(starts).doesNotContain(saturday(11, 0)).last().isEqualTo(saturday(16, 0));
    }

    @Test
    @Tag("RES-4")
    @DisplayName("At Saturday 7:00, with 2 hours' notice, the earliest Check-in Period starts at 9:00, so the earliest start is 9:15.")
    void earliestStartLeavesTheMinimumNotice() {
        bed.time.set(saturday(7, 0));

        assertThat(startsFor(AISYAH_CUSTOMER_ID, 120)).first().isEqualTo(saturday(9, 15));
    }

    @Test
    @Tag("RES-5")
    @DisplayName("Aisyah can book 2h 15m but not 2h 10m: no start time is offered for a length off the steps.")
    void lengthOffTheBookingStepsHasNoStartTimes() {
        assertThat(startsFor(AISYAH_CUSTOMER_ID, 135)).isNotEmpty();
        assertThat(startsFor(AISYAH_CUSTOMER_ID, 130)).isEmpty();
    }

    @Test
    @Tag("RES-1")
    @DisplayName("Start times are found only for a registered customer and a listed provider.")
    void unknownCustomerOrProviderIsNotFound() {
        assertThatThrownBy(() -> startsFor("customer-unknown", 240)).isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> bed.findAvailableStartTimes.find(new FindAvailableStartTimesRequest(
                AISYAH_CUSTOMER_ID, "provider-unknown", SATURDAY, List.of(KITCHEN_CLEANING), 240, 2)))
                .isInstanceOf(ProviderNotFoundException.class);
    }
}
