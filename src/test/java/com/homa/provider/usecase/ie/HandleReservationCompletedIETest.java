package com.homa.provider.usecase.ie;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_RESERVATION;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.completed;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.rosterChanged;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.saturdayReserved;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.sundayReserved;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.provider.domain.ReservationId;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.usecase.ProviderWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationCompletedIETest {

    private static final TeamMemberId SITI = new TeamMemberId("member-siti");

    private final ProviderWorld world = new ProviderWorld();

    private void saturdayIsRosteredOnSiti() {
        world.reservationReserved.handle(saturdayReserved("reserved-1"));
        world.rosterChanged.handle(rosterChanged("roster-1", SATURDAY_RESERVATION, "member-siti"));
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Siti marks Saturday's job Completed: its window is dropped, so she holds nothing.")
    void completedReservationDropsItsWindow() {
        saturdayIsRosteredOnSiti();

        world.reservationCompleted.handle(completed("completed-1", SATURDAY_RESERVATION));

        assertThat(world.rosteredWindows.findByReservationId(new ReservationId(SATURDAY_RESERVATION))).isEmpty();
        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).isEmpty();
        assertThat(world.processedMessages.hasHandled("completed-1", "provider.ReservationCompleted")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Another reservation Siti is rostered on keeps its window when Saturday's is Completed.")
    void otherReservationsKeepTheirWindows() {
        saturdayIsRosteredOnSiti();
        world.reservationReserved.handle(sundayReserved("reserved-2"));
        world.rosterChanged.handle(rosterChanged("roster-2", "reservation-sunday", "member-siti"));

        world.reservationCompleted.handle(completed("completed-1", SATURDAY_RESERVATION));

        assertThat(world.rosteredWindows.count()).isEqualTo(1);
        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).hasSize(1);
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A Completed event for a reservation Provider holds no window for has nothing to drop: it changes nothing and isn't retried.")
    void completedEventForAnUnknownReservationChangesNothing() {
        saturdayIsRosteredOnSiti();

        world.reservationCompleted.handle(completed("completed-x", "reservation-unknown"));

        assertThat(world.rosteredWindows.count()).isEqualTo(1);
        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).hasSize(1);
        assertThat(world.processedMessages.hasHandled("completed-x", "provider.ReservationCompleted")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("The same Completed event delivered again changes nothing: a window set up since stays.")
    void redeliveredEventChangesNothing() {
        saturdayIsRosteredOnSiti();
        world.reservationCompleted.handle(completed("completed-1", SATURDAY_RESERVATION));
        saturdayIsRosteredOnSiti2();

        world.reservationCompleted.handle(completed("completed-1", SATURDAY_RESERVATION));

        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).hasSize(1);
        assertThat(world.publisher.published()).isEmpty();
    }

    private void saturdayIsRosteredOnSiti2() {
        world.reservationReserved.handle(saturdayReserved("reserved-again"));
        world.rosterChanged.handle(rosterChanged("roster-again", SATURDAY_RESERVATION, "member-siti"));
    }
}
