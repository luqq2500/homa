package com.homa.provider.usecase.ie;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_RESERVATION;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.cancelled;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.rosterChanged;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.saturdayReserved;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.provider.domain.ReservationId;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.usecase.ProviderWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationCancelledIETest {

    private static final TeamMemberId SITI = new TeamMemberId("member-siti");

    private final ProviderWorld world = new ProviderWorld();

    private void saturdayIsRosteredOnSiti() {
        world.reservationReserved.handle(saturdayReserved("reserved-1"));
        world.rosterChanged.handle(rosterChanged("roster-1", SATURDAY_RESERVATION, "member-siti"));
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Kemas Home cancels Aisyah's Saturday reservation: its window is dropped, so Siti holds nothing.")
    void cancelledReservationDropsItsWindow() {
        saturdayIsRosteredOnSiti();

        world.reservationCancelled.handle(cancelled("cancelled-1", SATURDAY_RESERVATION));

        assertThat(world.rosteredWindows.findByReservationId(new ReservationId(SATURDAY_RESERVATION))).isEmpty();
        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).isEmpty();
        assertThat(world.processedMessages.hasHandled("cancelled-1", "provider.ReservationCancelled")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A Cancelled event for a reservation Provider holds no window for has nothing to drop: it changes nothing and isn't retried.")
    void cancelledEventForAnUnknownReservationChangesNothing() {
        saturdayIsRosteredOnSiti();

        world.reservationCancelled.handle(cancelled("cancelled-x", "reservation-unknown"));

        assertThat(world.rosteredWindows.count()).isEqualTo(1);
        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).hasSize(1);
        assertThat(world.processedMessages.hasHandled("cancelled-x", "provider.ReservationCancelled")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("The same Cancelled event delivered again changes nothing: a window set up since stays.")
    void redeliveredEventChangesNothing() {
        saturdayIsRosteredOnSiti();
        world.reservationCancelled.handle(cancelled("cancelled-1", SATURDAY_RESERVATION));
        saturdayIsRosteredOnSitiAgain();

        world.reservationCancelled.handle(cancelled("cancelled-1", SATURDAY_RESERVATION));

        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).hasSize(1);
        assertThat(world.publisher.published()).isEmpty();
    }

    private void saturdayIsRosteredOnSitiAgain() {
        world.reservationReserved.handle(saturdayReserved("reserved-again"));
        world.rosterChanged.handle(rosterChanged("roster-again", SATURDAY_RESERVATION, "member-siti"));
    }
}
