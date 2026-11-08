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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import reservation.domain.exception.CancellationLockedDuringCheckInPeriodException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.ProviderMissedCheckInException;

/** Feature: Customer cancellation. Background: Saturday 10:00 to 13:00, hours fee RM180, travel RM20, total RM230. */
class ReservationCustomerCancellationTest {

    @Nested
    class WhenRequested {

        @ParameterizedTest(name = "cancelling a request on the {0} tier is free")
        @EnumSource(CancellationTier.class)
        void customerCancelsForFreeOnEveryTier(CancellationTier tier) {
            Reservation reservation = aReservation().onTier(tier).requested();

            Settlement settlement = reservation.cancelByCustomer(thursday("12:00"));

            assertThat(settlement.customerRefund()).isEqualTo(rm("230.00"));
            assertThat(settlement.providerEarning()).isEqualTo(rm("0.00"));
            assertThat(settlement.serviceFee()).isEqualTo(rm("0.00"));
            assertThat(settlement.ending()).isEqualTo(Ending.CUSTOMER_CANCELLED_REQUEST);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(reservation.ending()).contains(Ending.CUSTOMER_CANCELLED_REQUEST);
        }
    }

    @Nested
    class WhenReserved {

        static Stream<Arguments> cancellations() {
            return Stream.of(
                    Arguments.of(CancellationTier.FLEXIBLE, saturday("09:00"), "0.00", "0.00", "230.00"),
                    Arguments.of(CancellationTier.STANDARD, friday("09:59"), "0.00", "0.00", "230.00"),
                    Arguments.of(CancellationTier.STANDARD, friday("10:00"), "18.00", "2.70", "209.30"),
                    Arguments.of(CancellationTier.FIRM, saturday("09:59"), "36.00", "5.40", "188.60"));
        }

        @ParameterizedTest(name = "{0} cancelling at {1}: provider earns {2}, Homa {3}, customer refunded {4}")
        @MethodSource("cancellations")
        void cancellingAReservedBookingBeforeTheStartFollowsTheTier(
                CancellationTier tier, Instant at, String earning, String fee, String refund) {
            Settlement settlement = aReservation().onTier(tier).reserved().cancelByCustomer(at);

            assertThat(settlement.providerEarning()).isEqualTo(rm(earning));
            assertThat(settlement.serviceFee()).isEqualTo(rm(fee));
            assertThat(settlement.customerRefund()).isEqualTo(rm(refund));
        }

        @Test
        void cancellingBeforeTheFeeWindowIsFree() {
            Reservation reservation = aReservation().onTier(CancellationTier.FIRM).reserved();

            Settlement settlement = reservation.cancelByCustomer(friday("09:59"));

            assertThat(settlement.ending()).isEqualTo(Ending.CUSTOMER_CANCELLED_FREE);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void cancellingInsideTheFeeWindowPaysTheTierFee() {
            Settlement settlement = aReservation().onTier(CancellationTier.STANDARD).reserved().cancelByCustomer(friday("10:00"));

            assertThat(settlement.ending()).isEqualTo(Ending.CUSTOMER_CANCELLED_IN_FEE_WINDOW);
            assertThat(settlement.total()).isEqualTo(rm("230.00"));
        }

        @Test
        void withTravelIncludedTheTierAppliesToTheFullProviderPrice() {
            Reservation reservation = aReservation().withTravelIncluded("35").onTier(CancellationTier.STANDARD).reserved();
            assertThat(reservation.price().providerPrice()).isEqualTo(rm("210.00"));
            assertThat(reservation.price().total()).isEqualTo(rm("241.50"));

            Settlement settlement = reservation.cancelByCustomer(saturday("09:00"));

            assertThat(settlement.providerEarning()).isEqualTo(rm("21.00"));
            assertThat(settlement.customerRefund()).isEqualTo(rm("217.35"));
        }

        @Test
        void aCancellationFeeIsReleasedStraightAway() {
            Settlement settlement = aReservation().onTier(CancellationTier.STANDARD).reserved().cancelByCustomer(saturday("09:00"));

            assertThat(settlement.releaseAt()).isEqualTo(saturday("09:00"));
        }

        @Test
        void customerCannotCancelDuringCheckInPeriodBeforeCheckIn() {
            Reservation reservation = aReservation().reserved();

            assertThatThrownBy(() -> reservation.cancelByCustomer(saturday("10:05")))
                    .isInstanceOf(CancellationLockedDuringCheckInPeriodException.class);
            assertThatThrownBy(() -> reservation.cancelByCustomer(saturday("10:00")))
                    .isInstanceOf(CancellationLockedDuringCheckInPeriodException.class);
            assertThatThrownBy(() -> reservation.cancelByCustomer(saturday("10:15")))
                    .isInstanceOf(CancellationLockedDuringCheckInPeriodException.class);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        }

        @Test
        void afterAMissedCheckInTheCustomerDoesNotCancelItEndsAsAProviderNoShow() {
            assertThatThrownBy(() -> aReservation().reserved().cancelByCustomer(saturday("10:15:01")))
                    .isInstanceOf(ProviderMissedCheckInException.class);
        }
    }

    @Nested
    class WhenCheckedIn {

        @Test
        void cancellingAfterCheckInCountsAsACustomerNoShow() {
            Reservation reservation = aReservation().checkedInAt(saturday("09:58"));

            Settlement settlement = reservation.cancelByCustomer(saturday("10:20"));

            assertThat(settlement.providerEarning()).isEqualTo(rm("74.00"));
            assertThat(settlement.serviceFee()).isEqualTo(rm("11.10"));
            assertThat(settlement.customerRefund()).isEqualTo(rm("144.90"));
            assertThat(settlement.ending()).isEqualTo(Ending.CUSTOMER_CANCELLED_AFTER_CHECK_IN);
            assertThat(settlement.releaseAt()).isEqualTo(saturday("10:20"));
            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void cancellingAfterAnEarlyCheckInBeforeTheStartIsStillACustomerNoShow() {
            Settlement settlement = aReservation().checkedInAt(saturday("09:50")).cancelByCustomer(saturday("09:55"));

            assertThat(settlement.providerEarning()).isEqualTo(rm("74.00"));
        }
    }

    @Test
    void anEndedReservationCannotBeCancelled() {
        Reservation reservation = aReservation().requested();
        reservation.decline(thursday("12:00"));

        assertThatThrownBy(() -> reservation.cancelByCustomer(thursday("13:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
