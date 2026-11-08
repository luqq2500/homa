package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.A_PHOTO;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.sunday;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.CompletionDeadlineNotReachedException;
import reservation.domain.exception.EvidenceRequiredException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.NotCheckedInException;
import reservation.domain.exception.ReservationDetailRequiredException;

/** Feature: Completion. Background: reserved for Saturday 10:00 to 13:00 and the lead checked in at 10:00. */
class ReservationCompletionTest {

    @Nested
    class Submitting {

        @Test
        void theProviderMarksTheJobCompletedWithEvidence() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

            reservation.submitCompletion(List.of(A_PHOTO), saturday("12:45"));

            assertThat(reservation.completion()).contains(new Completion(saturday("12:45"), List.of(A_PHOTO)));
            assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        }

        @Test
        void completionNeedsPhotoOrVideoEvidence() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

            assertThatThrownBy(() -> reservation.submitCompletion(List.of(), saturday("12:45")))
                    .isInstanceOf(EvidenceRequiredException.class);
            assertThat(reservation.completion()).isEmpty();
        }

        @Test
        void completionNeedsACheckInFirst() {
            assertThatThrownBy(() -> aReservation().reservedWithTeamNamed().submitCompletion(List.of(A_PHOTO), saturday("12:45")))
                    .isInstanceOf(NotCheckedInException.class);
        }

        @Test
        void completionIsSubmittedOnce() {
            Reservation reservation = aReservation().completedAt(saturday("12:45"));

            assertThatThrownBy(() -> reservation.submitCompletion(List.of(A_PHOTO), saturday("13:00")))
                    .isInstanceOf(CompletionAlreadySubmittedException.class);
        }

        @Test
        void aRequestCannotBeCompleted() {
            assertThatThrownBy(() -> aReservation().requested().submitCompletion(List.of(A_PHOTO), saturday("12:45")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void evidenceIsAReference() {
            assertThat(EvidenceRef.of(" evidence/after.mp4 ").reference()).isEqualTo("evidence/after.mp4");
            assertThatThrownBy(() -> EvidenceRef.of(" ")).isInstanceOf(ReservationDetailRequiredException.class);
        }
    }

    @Nested
    class AfterCompletion {

        @Test
        void aCompletedJobIsNoLongerCancelledOrANoShow() {
            assertThatThrownBy(() -> aReservation().completedAt(saturday("12:45")).cancelByCustomer(saturday("12:50")))
                    .isInstanceOf(CompletionAlreadySubmittedException.class);
            assertThatThrownBy(() -> aReservation().completedAt(saturday("12:45")).cancelByProvider(saturday("12:50")))
                    .isInstanceOf(CompletionAlreadySubmittedException.class);
            assertThatThrownBy(() -> aReservation().completedAt(saturday("10:30")).reportCustomerNoShow(saturday("10:45")))
                    .isInstanceOf(CompletionAlreadySubmittedException.class);
        }
    }

    @Nested
    class Reminders {

        static Stream<Arguments> reminders() {
            return Stream.of(Arguments.of(saturday("13:29"), false), Arguments.of(saturday("13:30"), true));
        }

        @ParameterizedTest(name = "the completion reminder is due at {0}: {1}")
        @MethodSource("reminders")
        void theCompletionReminderIsDue30MinutesAfterTheBookedEnd(Instant at, boolean due) {
            assertThat(aReservation().checkedInAt(saturday("10:00")).isCompletionReminderDue(at)).isEqualTo(due);
        }

        @Test
        void noReminderOnceCompletionIsSubmitted() {
            assertThat(aReservation().completedAt(saturday("12:45")).isCompletionReminderDue(saturday("13:30"))).isFalse();
        }

        @Test
        void noReminderForAReservationThatIsNoLongerReserved() {
            Reservation reservation = aReservation().reservedWithTeamNamed();
            reservation.markProviderNoShow(saturday("10:16"));

            assertThat(reservation.isCompletionReminderDue(saturday("13:30"))).isFalse();
        }
    }

    @Nested
    class TheCompletionDeadline {

        @Test
        void missingTheCompletionDeadlineSendsItToHoma() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

            reservation.missCompletionDeadline(sunday("13:00"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        }

        @Test
        void theCompletionDeadlineCannotBeMissedEarly() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

            assertThatThrownBy(() -> reservation.missCompletionDeadline(sunday("12:59")))
                    .isInstanceOf(CompletionDeadlineNotReachedException.class);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        }

        @Test
        void aSubmittedCompletionIsNotMissed() {
            assertThatThrownBy(() -> aReservation().completedAt(saturday("12:45")).missCompletionDeadline(sunday("13:00")))
                    .isInstanceOf(CompletionAlreadySubmittedException.class);
        }

        @Test
        void onlyAReservedBookingMissesItsDeadline() {
            assertThatThrownBy(() -> aReservation().requested().missCompletionDeadline(sunday("13:00")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }
}
