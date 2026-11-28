package com.homa.provider.usecase.ie;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_CHECK_IN_START;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_JOB_END;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_RESERVATION;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.rosterChanged;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.saturdayReserved;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.TimeInterval;
import com.homa.provider.domain.ReservationId;
import com.homa.provider.domain.RosteredReservation;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.usecase.ProviderWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationReservedIETest {

    private static final ReservationId SATURDAY = new ReservationId(SATURDAY_RESERVATION);

    private final ProviderWorld world = new ProviderWorld();

    @Test
    @Tag("HLP-9")
    @DisplayName("Aisyah's Saturday job is Reserved: Provider keeps its window from the Check-in Period at 9:45 to the job end at 14:00, with nobody rostered yet.")
    void reservedReservationKeepsItsWindow() {
        world.reservationReserved.handle(saturdayReserved("reserved-1"));

        RosteredReservation kept = world.rosteredWindows.findByReservationId(SATURDAY).orElseThrow();
        assertThat(kept.reservationId()).isEqualTo(SATURDAY);
        assertThat(kept.window()).isEqualTo(new TimeInterval(SATURDAY_CHECK_IN_START, SATURDAY_JOB_END));
        assertThat(kept.roster()).isEmpty();
        assertThat(world.processedMessages.hasHandled("reserved-1", "provider.ReservationReserved")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("The same Reserved event delivered again changes nothing: the roster Kemas Home set meanwhile stays.")
    void redeliveredEventChangesNothing() {
        world.reservationReserved.handle(saturdayReserved("reserved-1"));
        world.rosterChanged.handle(rosterChanged("roster-1", SATURDAY_RESERVATION, "member-siti", "member-nurul"));

        world.reservationReserved.handle(saturdayReserved("reserved-1"));

        assertThat(world.rosteredWindows.findByReservationId(SATURDAY).orElseThrow().roster())
                .containsExactlyInAnyOrder(new TeamMemberId("member-siti"), new TeamMemberId("member-nurul"));
        assertThat(world.rosteredWindows.count()).isEqualTo(1);
        assertThat(world.publisher.published()).isEmpty();
    }
}
