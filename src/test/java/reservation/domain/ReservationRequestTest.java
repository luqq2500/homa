package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.anOffering;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.theLaunchPolicy;
import static reservation.domain.ReservationBuilders.thursday;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import kernel.Money;
import kernel.Percentage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reservation.domain.exception.CityNotCoveredException;
import reservation.domain.exception.HelpersOutsideProviderRangeException;
import reservation.domain.exception.HoursOutsideProviderRangeException;
import reservation.domain.exception.JobNotOfferedException;
import reservation.domain.exception.ListingPausedException;
import reservation.domain.exception.ReservationDetailRequiredException;
import reservation.domain.exception.StartBeyondHorizonException;
import reservation.domain.exception.StartNotOnQuarterHourException;
import reservation.domain.exception.StartTooSoonException;

/** Feature: Requesting a reservation. Background: it is Thursday 09:00, and a live provider in Cyberjaya has hours 2 to 4 and helpers 1 to 3. */
class ReservationRequestTest {

    private static final Instant NOW = thursday("09:00");

    private static Reservation request(ReservationBuilders.DetailsBuilder booking) {
        return Reservation.request(
                "customer-1", booking.build(), anOffering().build(), theLaunchPolicy(), Optional.empty(), PaymentMethod.CARD, VERIFIED_UNTIL, NOW);
    }

    static Stream<Arguments> requests() {
        return Stream.of(
                Arguments.of(3, 2, saturday("10:00"), null),
                Arguments.of(1, 2, saturday("10:00"), HoursOutsideProviderRangeException.class),
                Arguments.of(3, 4, saturday("10:00"), HelpersOutsideProviderRangeException.class),
                Arguments.of(3, 2, saturday("10:10"), StartNotOnQuarterHourException.class),
                Arguments.of(3, 2, thursday("10:30"), StartTooSoonException.class),
                Arguments.of(3, 2, NOW.plus(Duration.ofDays(31)), StartBeyondHorizonException.class));
    }

    @ParameterizedTest(name = "{0} hours with {1} helpers starting {2}: {3}")
    @MethodSource("requests")
    void requestDetailsMustFitHomasAndTheProvidersRules(int hours, int helpers, Instant start, Class<? extends Throwable> rejection) {
        ReservationBuilders.DetailsBuilder booking = aBooking().forHours(hours).withHelpers(helpers).startingAt(start);

        if (rejection == null) {
            assertThat(request(booking).status()).isEqualTo(ReservationStatus.REQUESTED);
        } else {
            assertThatThrownBy(() -> request(booking)).isInstanceOf(rejection);
        }
    }

    @Nested
    class Boundaries {

        @Test
        void theProvidersMinimumAndMaximumHoursCanBeBooked() {
            assertThat(request(aBooking().forHours(2)).status()).isEqualTo(ReservationStatus.REQUESTED);
            assertThat(request(aBooking().forHours(4)).status()).isEqualTo(ReservationStatus.REQUESTED);
            assertThatThrownBy(() -> request(aBooking().forHours(5))).isInstanceOf(HoursOutsideProviderRangeException.class);
        }

        @Test
        void theProvidersMinimumAndMaximumHelpersCanBeBooked() {
            assertThat(request(aBooking().withHelpers(1)).status()).isEqualTo(ReservationStatus.REQUESTED);
            assertThat(request(aBooking().withHelpers(3)).status()).isEqualTo(ReservationStatus.REQUESTED);
            assertThatThrownBy(() -> request(aBooking().withHelpers(0)))
                    .isInstanceOf(HelpersOutsideProviderRangeException.class);
        }

        @Test
        void aStartExactlyTheMinimumNoticeAwayCanBeBooked() {
            assertThat(request(aBooking().startingAt(thursday("11:00"))).status()).isEqualTo(ReservationStatus.REQUESTED);
            assertThatThrownBy(() -> request(aBooking().startingAt(thursday("10:45"))))
                    .isInstanceOf(StartTooSoonException.class);
        }

        @Test
        void aStartExactlyAtTheBookingHorizonCanBeBooked() {
            Instant horizon = NOW.plus(Duration.ofDays(30));

            assertThat(request(aBooking().startingAt(horizon)).status()).isEqualTo(ReservationStatus.REQUESTED);
            assertThatThrownBy(() -> request(aBooking().startingAt(horizon.plus(Duration.ofMinutes(15)))))
                    .isInstanceOf(StartBeyondHorizonException.class);
        }

        @Test
        void aStartBetweenSecondsIsNotOnAQuarterHour() {
            assertThatThrownBy(() -> request(aBooking().startingAt(saturday("10:00").plusNanos(1))))
                    .isInstanceOf(StartNotOnQuarterHourException.class);
        }
    }

    @Test
    void jobsMustBeOfferedByTheProvider() {
        assertThatThrownBy(() -> request(aBooking().forJobs("KITCHEN", "WARDROBE_ORGANISE")))
                .isInstanceOf(JobNotOfferedException.class)
                .hasMessageContaining("WARDROBE_ORGANISE");
    }

    @Test
    void theCustomersCityMustBeCovered() {
        assertThatThrownBy(() -> request(aBooking().in("Putrajaya")))
                .isInstanceOf(CityNotCoveredException.class)
                .hasMessageContaining("Putrajaya");
    }

    @Test
    void theReservationKeepsThePolicyInForceWhenRequested() {
        Reservation underVersion1 = request(aBooking());
        PolicySnapshot v1 = theLaunchPolicy();
        PolicySnapshot version2 = new PolicySnapshot(
                2, Percentage.of(12), v1.minimumNotice(), v1.bookingHorizon(), v1.requestExpiry(), v1.feeWindow(),
                v1.cancellationFees(), v1.providerLateCancellationFine(), v1.providerNoShowFine(), v1.earliestCheckIn(),
                v1.checkInPeriod(), v1.checkInRadiusMetres(), v1.customerNoShowWait(), v1.customerNoShowFee(),
                v1.noShowReportWindow(), v1.completionReminder(), v1.completionDeadline(), v1.disputeWindowExtension(),
                v1.teamReminder());

        Reservation underVersion2 = Reservation.request(
                "customer-2", aBooking().build(), anOffering().build(), version2, Optional.empty(), PaymentMethod.CARD, VERIFIED_UNTIL, NOW);

        assertThat(underVersion1.policy().version()).isEqualTo(1);
        assertThat(underVersion1.price().serviceFee()).isEqualTo(Money.parse("30.00"));
        assertThat(underVersion2.policy().version()).isEqualTo(2);
        assertThat(underVersion2.price().serviceFee()).isEqualTo(Money.parse("24.00"));
    }

    @Test
    void theReservationKeepsWhoWhatAndHowItIsPaid() {
        Reservation reservation = Reservation.request(
                "customer-1", aBooking().build(), anOffering().build(), theLaunchPolicy(), Optional.of("enquiry-1"), PaymentMethod.FPX, VERIFIED_UNTIL, NOW);

        assertThat(reservation.id()).isNotBlank();
        assertThat(reservation.customerId()).isEqualTo("customer-1");
        assertThat(reservation.providerId()).isEqualTo("provider-1");
        assertThat(reservation.details()).isEqualTo(aBooking().build());
        assertThat(reservation.offering()).isEqualTo(anOffering().build());
        assertThat(reservation.enquiryId()).contains("enquiry-1");
        assertThat(reservation.paymentMethod()).isEqualTo(PaymentMethod.FPX);
        assertThat(reservation.requestedAt()).isEqualTo(NOW);
    }

    @Test
    void aPausedListingRefusesNewRequestsButKeepsAcceptedOnes() {
        Reservation accepted = request(aBooking());
        accepted.accept(VERIFIED_UNTIL, NOW.plus(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> Reservation.request(
                        "customer-2", aBooking().build(), anOffering().paused().build(), theLaunchPolicy(), Optional.empty(),
                        PaymentMethod.CARD, VERIFIED_UNTIL, NOW))
                .isInstanceOf(ListingPausedException.class)
                .hasMessageContaining("provider-1");
        assertThat(accepted.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(accepted.offering().takesRequests()).isTrue();
    }

    @Test
    void aRequestNeedsACustomerAndAPaymentMethod() {
        assertThatThrownBy(() -> Reservation.request(
                        " ", aBooking().build(), anOffering().build(), theLaunchPolicy(), Optional.empty(), PaymentMethod.CARD, VERIFIED_UNTIL, NOW))
                .isInstanceOf(ReservationDetailRequiredException.class);
        assertThatThrownBy(() -> Reservation.request(
                        "customer-1", aBooking().build(), anOffering().build(), theLaunchPolicy(), Optional.empty(), null, VERIFIED_UNTIL, NOW))
                .isInstanceOf(NullPointerException.class);
    }
}
