package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.InvalidDecisionAmountException;
import reservation.domain.exception.InvalidStatusTransitionException;

/** Feature: Homa's decision. */
class ReservationDecisionTest {

    @Nested
    class AJobDispute {

        /** Disputed with provider price RM200 and total RM230. */
        private Reservation aDisputedReservation() {
            Reservation reservation = aReservation().completedAt(saturday("12:45"));
            reservation.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(), saturday("13:30"));
            return reservation;
        }

        @Test
        void homaReleasesThePaymentAndTheJobIsCompleted() {
            Reservation reservation = aDisputedReservation();

            Settlement settlement = reservation.resolve(CaseDecision.release(), saturday("18:00"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.COMPLETED);
            assertThat(reservation.ending()).contains(Ending.DECIDED_BY_HOMA);
            assertThat(settlement.providerEarning()).isEqualTo(rm("200.00"));
            assertThat(settlement.customerRefund()).isEqualTo(rm("0.00"));
            assertThat(settlement.releaseAt()).isEqualTo(saturday("18:00"));
        }

        @Test
        void aPartialRefundSplitsThePaymentAndTheServiceFeeFollowsTheProvider() {
            Settlement settlement = aDisputedReservation().resolve(CaseDecision.partialRefund(rm("100.00")), saturday("18:00"));

            assertThat(settlement.providerEarning()).isEqualTo(rm("100.00"));
            assertThat(settlement.serviceFee()).isEqualTo(rm("15.00"));
            assertThat(settlement.customerRefund()).isEqualTo(rm("115.00"));
        }

        @Test
        void aFullRefundReturnsEverythingToTheCustomer() {
            Settlement settlement = aDisputedReservation().resolve(CaseDecision.fullRefund(), saturday("18:00"));

            assertThat(settlement.customerRefund()).isEqualTo(rm("230.00"));
            assertThat(settlement.providerEarning()).isEqualTo(rm("0.00"));
        }

        @Test
        void aPartialRefundCannotGiveTheProviderMoreThanTheProviderPrice() {
            Reservation reservation = aDisputedReservation();

            assertThatThrownBy(() -> reservation.resolve(CaseDecision.partialRefund(rm("201.00")), saturday("18:00")))
                    .isInstanceOf(InvalidDecisionAmountException.class);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        }

        @Test
        void aPartialRefundCanGiveTheProviderFromNothingToTheProviderPrice() {
            assertThat(aDisputedReservation().resolve(CaseDecision.partialRefund(rm("200.00")), saturday("18:00")).providerEarning())
                    .isEqualTo(rm("200.00"));
            assertThat(aDisputedReservation().resolve(CaseDecision.partialRefund(rm("0.00")), saturday("18:00")).customerRefund())
                    .isEqualTo(rm("230.00"));
            assertThatThrownBy(() -> aDisputedReservation().resolve(CaseDecision.partialRefund(rm("-0.01")), saturday("18:00")))
                    .isInstanceOf(InvalidDecisionAmountException.class);
        }

        @Test
        void aMissedCompletionIsDecidedTheSameWay() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
            reservation.missCompletionDeadline(ReservationBuilders.sunday("13:00"));

            Settlement settlement = reservation.resolve(CaseDecision.partialRefund(rm("50.00")), ReservationBuilders.sunday("15:00"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.COMPLETED);
            assertThat(settlement.providerEarning()).isEqualTo(rm("50.00"));
        }
    }

    @Nested
    class AChallengedNoShow {

        private Reservation aChallengedNoShow() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
            reservation.reportCustomerNoShow(saturday("10:15"));
            reservation.challengeNoShow(saturday("10:45"));
            return reservation;
        }

        @Test
        void homaUpholdsTheNoShowAndTheReservationEndsCancelled() {
            Reservation reservation = aChallengedNoShow();

            Settlement settlement = reservation.resolve(CaseDecision.release(), saturday("15:00"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(settlement.providerEarning()).isEqualTo(rm("74.00"));
            assertThat(settlement.customerRefund()).isEqualTo(rm("144.90"));
        }

        @Test
        void homaFindsTheNoShowWrongfulAndRefundsInFull() {
            Reservation reservation = aChallengedNoShow();

            Settlement settlement = reservation.resolve(CaseDecision.fullRefund(), saturday("15:00"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(settlement.customerRefund()).isEqualTo(rm("230.00"));
            assertThat(settlement.providerEarning()).isEqualTo(rm("0.00"));
        }

        @Test
        void aPartialRefundCannotGiveTheProviderMoreThanTheNoShowFee() {
            assertThatThrownBy(() -> aChallengedNoShow().resolve(CaseDecision.partialRefund(rm("74.01")), saturday("15:00")))
                    .isInstanceOf(InvalidDecisionAmountException.class);
        }
    }

    @Test
    void onlyADisputedReservationIsDecided() {
        assertThatThrownBy(() -> aReservation().completedAt(saturday("12:45")).resolve(CaseDecision.release(), saturday("18:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void aDecisionNamesItsOutcome() {
        assertThat(CaseDecision.release().outcome()).isEqualTo(CaseDecision.Outcome.RELEASE);
        assertThat(CaseDecision.fullRefund().providerReceives()).isEmpty();
        assertThat(CaseDecision.partialRefund(rm("100")).providerReceives()).contains(rm("100"));
    }
}
