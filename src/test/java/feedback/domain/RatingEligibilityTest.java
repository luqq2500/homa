package feedback.domain;

import static feedback.domain.FeedbackBuilders.LAUNCH_LIMITS;
import static feedback.domain.FeedbackBuilders.stars;
import static feedback.domain.FeedbackBuilders.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feedback.domain.exception.FeedbackDetailRequiredException;
import feedback.domain.exception.ReservationNotRateableException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

/** Feature: What can be rated. */
class RatingEligibilityTest {

    @ParameterizedTest(name = "a {0} reservation cannot be rated")
    @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "DISPUTED", "RESERVED", "REQUESTED", "DECLINED", "EXPIRED"})
    void onlyCompletedReservationsCanBeRated(ReservationStatus status) {
        RateableReservation reservation = RateableReservation.notCompleted("R1", status, "C1", "P1", Set.of("aina"));

        assertThatThrownBy(() -> Rating.submit(reservation, "C1", stars(4), Map.of(), LAUNCH_LIMITS, sunday("10:00")))
                .isInstanceOf(ReservationNotRateableException.class)
                .hasMessageContaining(status.name());
    }

    @Test
    void aReservationNotYetCompletedHasNoCompletionTime() {
        RateableReservation reserved = RateableReservation.notCompleted("R1", ReservationStatus.RESERVED, "C1", "P1", Set.of());

        assertThat(reserved.completedAt()).isEmpty();
        assertThat(reserved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThatThrownBy(() -> RateableReservation.notCompleted("R1", ReservationStatus.COMPLETED, "C1", "P1", Set.of()))
                .isInstanceOf(FeedbackDetailRequiredException.class);
    }

    @Test
    void aCompletedReservationKnowsWhoAndWhen() {
        RateableReservation completed = FeedbackBuilders.aCompletedReservation();

        assertThat(completed.reservationId()).isEqualTo("R1");
        assertThat(completed.customerId()).isEqualTo("C1");
        assertThat(completed.providerId()).isEqualTo("P1");
        assertThat(completed.namedHelperIds()).containsExactlyInAnyOrder("aina", "ben");
        assertThat(completed.completedAt()).contains(FeedbackBuilders.saturday("14:00"));
        assertThat(completed.isRated()).isFalse();
        assertThat(completed.rated().isRated()).isTrue();
        assertThat(completed.rated().completedAt()).isEqualTo(completed.completedAt());
    }

    @Test
    void aReservationBelongsToACustomerAndAProvider() {
        assertThatThrownBy(() -> RateableReservation.completed(" ", "C1", "P1", Set.of(), sunday("10:00")))
                .isInstanceOf(FeedbackDetailRequiredException.class);
        assertThatThrownBy(() -> RateableReservation.completed("R1", null, "P1", Set.of(), sunday("10:00")))
                .isInstanceOf(FeedbackDetailRequiredException.class);
        assertThatThrownBy(() -> RateableReservation.completed("R1", "C1", "", Set.of(), sunday("10:00")))
                .isInstanceOf(FeedbackDetailRequiredException.class);
        assertThatThrownBy(() -> RateableReservation.completed("R1", "C1", "P1", Set.of(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
