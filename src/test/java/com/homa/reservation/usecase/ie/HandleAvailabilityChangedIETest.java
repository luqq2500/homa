package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.AvailabilityChangedIE;
import com.homa.reservation.domain.replica.DailyHours;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.domain.replica.WorkingHours;
import com.homa.reservation.usecase.ReservationTestBed;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleAvailabilityChangedIETest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private WorkingHours hoursOf(String providerId) {
        return bed.providers.findById(providerId).map(ProviderReplica::workingHours).orElseThrow();
    }

    @Test
    @Tag("RES-3")
    @DisplayName("Kemas Home now works Saturday 9:00–13:00 and Sunday 10:00–16:00, and no other day.")
    void weeklyHoursReplaceTheOldOnes() {
        bed.availabilityChanged.handle(new AvailabilityChangedIE(bed.nextInboundId(), KEMAS_HOME_PROVIDER_ID,
                List.of("SATURDAY 09:00-13:00", "SUNDAY 10:00-16:00"), TAPPED_PAY_AT));

        assertThat(hoursOf(KEMAS_HOME_PROVIDER_ID)).isEqualTo(new WorkingHours(Map.of(
                DayOfWeek.SATURDAY, new DailyHours(LocalTime.of(9, 0), LocalTime.of(13, 0)),
                DayOfWeek.SUNDAY, new DailyHours(LocalTime.of(10, 0), LocalTime.of(16, 0)))));
        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow().listing()).isPresent();
    }

    @Test
    @Tag("RES-3")
    @DisplayName("Hours heard before the listing are kept for when it arrives.")
    void hoursOfAProviderNotYetListedAreKept() {
        bed.availabilityChanged.handle(bed.kemasHours("provider-new"));

        assertThat(hoursOf("provider-new")).isEqualTo(hoursOf(KEMAS_HOME_PROVIDER_ID));
        assertThat(bed.providers.findById("provider-new").orElseThrow().listing()).isEmpty();
    }

    @Test
    @Tag("RES-3")
    @DisplayName("Redelivered hours change nothing, even after newer ones.")
    void redeliveredHoursAreIgnored() {
        AvailabilityChangedIE original = bed.kemasHours(KEMAS_HOME_PROVIDER_ID);
        bed.availabilityChanged.handle(original);
        bed.availabilityChanged.handle(new AvailabilityChangedIE(bed.nextInboundId(), KEMAS_HOME_PROVIDER_ID,
                List.of(), TAPPED_PAY_AT));

        bed.availabilityChanged.handle(original);

        assertThat(hoursOf(KEMAS_HOME_PROVIDER_ID)).isEqualTo(new WorkingHours(Map.of()));
        assertThat(HandleAvailabilityChangedIE.HANDLER).isEqualTo("reservation.AvailabilityChanged");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
