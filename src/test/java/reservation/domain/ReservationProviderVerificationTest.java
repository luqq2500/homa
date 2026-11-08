package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.anOffering;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.theLaunchPolicy;
import static reservation.domain.ReservationBuilders.thursday;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.JobEndsAfterProviderVerificationException;

/**
 * Feature: Verified for the whole job. The provider is requested and accepts only for a job that ends by
 * their re-verification due date. The booking is Saturday 10:00 for 3 hours, so it ends at 13:00.
 */
class ReservationProviderVerificationTest {

    private static final Instant NOW = thursday("09:00");

    private static Reservation requestWithProviderDueAt(Instant verifiedUntil) {
        return Reservation.request(
                "customer-1", aBooking().build(), anOffering().build(), theLaunchPolicy(), Optional.empty(),
                PaymentMethod.CARD, verifiedUntil, NOW);
    }

    @Nested
    class Requesting {

        @Test
        void aJobEndingAfterTheProvidersDueDateCannotBeRequested() {
            assertThatThrownBy(() -> requestWithProviderDueAt(saturday("12:59")))
                    .isInstanceOf(JobEndsAfterProviderVerificationException.class)
                    .hasMessageContaining(saturday("13:00").toString())
                    .hasMessageContaining(saturday("12:59").toString());
        }

        @Test
        void aJobEndingExactlyAtTheDueDateCanBeRequested() {
            Reservation reservation = requestWithProviderDueAt(saturday("13:00"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
        }

        @Test
        void aJobStartingBeforeButEndingAfterTheDueDateCannotBeRequested() {
            assertThatThrownBy(() -> requestWithProviderDueAt(saturday("11:00")))
                    .isInstanceOf(JobEndsAfterProviderVerificationException.class);
        }
    }

    @Nested
    class Editing {

        @Test
        void anEditCannotMoveTheEndPastTheDueDate() {
            Reservation reservation = requestWithProviderDueAt(saturday("13:00"));

            assertThatThrownBy(() -> reservation.edit(aBooking().forHours(4).build(), saturday("13:00"), thursday("10:00")))
                    .isInstanceOf(JobEndsAfterProviderVerificationException.class);
            assertThat(reservation.details()).isEqualTo(aBooking().build());
        }

        @Test
        void anEditEndingExactlyAtTheDueDateIsAllowed() {
            Reservation reservation = requestWithProviderDueAt(saturday("14:00"));

            reservation.edit(aBooking().forHours(4).build(), saturday("14:00"), thursday("10:00"));

            assertThat(reservation.details().hours()).isEqualTo(4);
        }
    }

    @Nested
    class Accepting {

        @Test
        void theProviderCannotAcceptAJobEndingAfterTheirDueDateAsItIsNow() {
            Reservation reservation = aReservation().requested();

            assertThatThrownBy(() -> reservation.accept(saturday("12:00"), thursday("12:00")))
                    .isInstanceOf(JobEndsAfterProviderVerificationException.class);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
        }

        @Test
        void theProviderCanAcceptOnceAPassedRecheckMovesTheDueDateLater() {
            Reservation reservation = aReservation().requested();
            assertThatThrownBy(() -> reservation.accept(saturday("12:00"), thursday("12:00")))
                    .isInstanceOf(JobEndsAfterProviderVerificationException.class);

            reservation.accept(VERIFIED_UNTIL, thursday("14:00"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        }

        @Test
        void aJobEndingExactlyAtTheDueDateCanBeAccepted() {
            Reservation reservation = aReservation().requested();

            reservation.accept(saturday("13:00"), thursday("12:00"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        }
    }
}
