package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import java.time.Instant;
import java.util.stream.Stream;
import kernel.Money;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import reservation.domain.exception.InvalidStatusTransitionException;

/**
 * Feature: Showing the cancellation terms. Background: Saturday 10:00 to 13:00, hours fee RM180, travel
 * RM20, total RM230.
 */
class ReservationCancellationTermsTest {

    @Nested
    class TheFeeAndWhenItStarts {

        static Stream<Arguments> terms() {
            return Stream.of(
                    Arguments.of(CancellationTier.FLEXIBLE, saturday("10:00"), "0.00", false),
                    Arguments.of(CancellationTier.STANDARD, friday("10:00"), "18.00", true),
                    Arguments.of(CancellationTier.FIRM, friday("10:00"), "36.00", true));
        }

        @ParameterizedTest(name = "on the {0} tier it is free until {1}, then the customer pays {2}")
        @MethodSource("terms")
        void theTermsNameWhenTheFeeStartsAndHowMuchItIs(CancellationTier tier, Instant freeUntil, String fee, boolean charges) {
            CancellationTerms terms = aReservation().onTier(tier).reserved().cancellationTerms();

            assertThat(terms.tier()).isEqualTo(tier);
            assertThat(terms.freeUntil()).isEqualTo(freeUntil);
            assertThat(terms.feeFromThen()).isEqualTo(rm(fee));
            assertThat(terms.chargesAFee()).isEqualTo(charges);
        }

        @Test
        void withTravelIncludedTheFeeIsAShareOfTheFullProviderPrice() {
            CancellationTerms terms = aReservation().withTravelIncluded("35").onTier(CancellationTier.STANDARD).reserved()
                    .cancellationTerms();

            assertThat(terms.feeFromThen()).isEqualTo(rm("21.00"));
            assertThat(terms.afterCheckIn()).isEqualTo(rm("63.00"));
        }

        @Test
        void theTermsAreShownBeforeTheProviderAcceptsToo() {
            CancellationTerms terms = aReservation().onTier(CancellationTier.FIRM).requested().cancellationTerms();

            assertThat(terms.freeUntil()).isEqualTo(friday("10:00"));
            assertThat(terms.feeFromThen()).isEqualTo(rm("36.00"));
        }
    }

    @Nested
    class TheTermsMatchWhatCancellingCosts {

        @ParameterizedTest(name = "on the {0} tier, cancelling just before the fee starts is free")
        @EnumSource(CancellationTier.class)
        void cancellingJustBeforeTheFeeStartsIsFree(CancellationTier tier) {
            Reservation reservation = aReservation().onTier(tier).reserved();
            Instant justBefore = reservation.cancellationTerms().freeUntil().minusSeconds(1);

            assertThat(reservation.cancelByCustomer(justBefore).providerEarning()).isEqualTo(rm("0.00"));
        }

        @ParameterizedTest(name = "on the {0} tier, cancelling when the fee starts pays the fee shown")
        @EnumSource(value = CancellationTier.class, names = {"STANDARD", "FIRM"})
        void cancellingWhenTheFeeStartsPaysTheFeeShown(CancellationTier tier) {
            Reservation reservation = aReservation().onTier(tier).reserved();
            CancellationTerms terms = reservation.cancellationTerms();

            assertThat(reservation.cancelByCustomer(terms.freeUntil()).providerEarning()).isEqualTo(terms.feeFromThen());
        }

        @Test
        void cancellingJustBeforeTheStartPaysTheFeeShown() {
            Reservation reservation = aReservation().onTier(CancellationTier.FIRM).reserved();
            Money shown = reservation.cancellationTerms().feeFromThen();

            assertThat(reservation.cancelByCustomer(saturday("09:59:59")).providerEarning()).isEqualTo(shown);
        }
    }

    @Nested
    class FromTheBookedStart {

        @Test
        void cancellingIsLockedFromTheBookedStartUntilTheCheckInPeriodEnds() {
            CancellationTerms terms = aReservation().reserved().cancellationTerms();

            assertThat(terms.lockedFrom()).isEqualTo(saturday("10:00"));
            assertThat(terms.lockedUntil()).isEqualTo(saturday("10:15"));
        }

        @Test
        void cancellingAfterCheckInCostsTheCustomerNoShowFee() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
            Money shown = reservation.cancellationTerms().afterCheckIn();

            assertThat(shown).isEqualTo(rm("74.00"));
            assertThat(reservation.cancelByCustomer(saturday("10:20")).providerEarning()).isEqualTo(shown);
        }
    }

    @Test
    void anEndedReservationHasNoCancellationTerms() {
        Reservation reservation = aReservation().reserved();
        reservation.cancelByCustomer(thursday("12:00"));

        assertThatThrownBy(reservation::cancellationTerms).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void cancellationTermsNeedEveryPart() {
        CancellationTerms terms = aReservation().reserved().cancellationTerms();

        assertThatThrownBy(() -> new CancellationTerms(null, terms.freeUntil(), terms.feeFromThen(), terms.lockedFrom(),
                        terms.lockedUntil(), terms.afterCheckIn()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CancellationTerms(terms.tier(), null, terms.feeFromThen(), terms.lockedFrom(),
                        terms.lockedUntil(), terms.afterCheckIn()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CancellationTerms(terms.tier(), terms.freeUntil(), null, terms.lockedFrom(),
                        terms.lockedUntil(), terms.afterCheckIn()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CancellationTerms(terms.tier(), terms.freeUntil(), terms.feeFromThen(), null,
                        terms.lockedUntil(), terms.afterCheckIn()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CancellationTerms(terms.tier(), terms.freeUntil(), terms.feeFromThen(), terms.lockedFrom(),
                        null, terms.afterCheckIn()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CancellationTerms(terms.tier(), terms.freeUntil(), terms.feeFromThen(), terms.lockedFrom(),
                        terms.lockedUntil(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
