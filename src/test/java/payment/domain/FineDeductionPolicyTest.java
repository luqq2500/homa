package payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static payment.domain.PaymentBuilders.NOW;
import static payment.domain.PaymentBuilders.aFineOf;
import static payment.domain.PaymentBuilders.aPayoutOf;
import static payment.domain.PaymentBuilders.monday;
import static payment.domain.PaymentBuilders.rm;
import static payment.domain.PaymentBuilders.tuesday;

import java.util.List;
import kernel.Money;
import kernel.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import payment.domain.exception.FineOfAnotherProviderException;
import payment.domain.exception.PayoutAlreadyDeductedException;

/** Feature: Fines and payouts. */
class FineDeductionPolicyTest {

    private static final Percentage HALF = Percentage.of(50);

    @Test
    void aFineIsDeductedAtMostHalfOfAPayout() {
        Fine fine = aFineOf("54.00", monday());

        Payout payout = FineDeductionPolicy.apply(aPayoutOf("80.00"), List.of(fine), HALF);

        assertThat(payout.deducted()).isEqualTo(rm("40.00"));
        assertThat(payout.net()).isEqualTo(rm("40.00"));
        assertThat(fine.outstanding()).isEqualTo(rm("14.00"));
        assertThat(payout.deductions()).containsExactly(new FineDeduction(fine.id(), rm("40.00")));
    }

    @Test
    void theRestOfTheFineComesOffTheNextPayout() {
        Fine fine = aFineOf("54.00", monday());
        FineDeductionPolicy.apply(aPayoutOf("80.00"), List.of(fine), HALF);

        Payout next = FineDeductionPolicy.apply(aPayoutOf("100.00"), List.of(fine), HALF);

        assertThat(next.deducted()).isEqualTo(rm("14.00"));
        assertThat(next.net()).isEqualTo(rm("86.00"));
        assertThat(fine.isCleared()).isTrue();
        assertThat(fine.deducted()).isEqualTo(rm("54.00"));
    }

    @Test
    void olderFinesAreDeductedFirst() {
        Fine tuesdays = aFineOf("20.00", tuesday());
        Fine mondays = aFineOf("10.00", monday());

        Payout payout = FineDeductionPolicy.apply(aPayoutOf("30.00"), List.of(tuesdays, mondays), HALF);

        assertThat(mondays.deducted()).isEqualTo(rm("10.00"));
        assertThat(tuesdays.deducted()).isEqualTo(rm("5.00"));
        assertThat(payout.deductions()).containsExactly(
                new FineDeduction(mondays.id(), rm("10.00")), new FineDeduction(tuesdays.id(), rm("5.00")));
    }

    @Test
    void aWaivedFineIsNotDeducted() {
        Fine fine = aFineOf("36.00", monday());
        fine.waive("Genuine emergency", NOW);

        Payout payout = FineDeductionPolicy.apply(aPayoutOf("100.00"), List.of(fine), HALF);

        assertThat(payout.deducted()).isEqualTo(rm("0.00"));
        assertThat(payout.deductions()).isEmpty();
        assertThat(payout.net()).isEqualTo(rm("100.00"));
    }

    @Test
    void aPayoutWithNoFinesIsPaidInFull() {
        assertThat(FineDeductionPolicy.apply(aPayoutOf("100.00"), List.of(), HALF).net()).isEqualTo(rm("100.00"));
    }

    @ParameterizedTest(name = "a payout for a {0} has no fines deducted")
    @EnumSource(value = PayoutKind.class, names = {"CANCELLATION_FEE", "CUSTOMER_NO_SHOW_FEE"})
    void onlyPayoutsForCompletedJobsHaveFinesDeducted(PayoutKind kind) {
        Fine fine = aFineOf("54.00", monday());

        Payout payout = FineDeductionPolicy.apply(Payout.create("provider-1", "reservation-1", rm("74.00"), kind), List.of(fine), HALF);

        assertThat(payout.net()).isEqualTo(rm("74.00"));
        assertThat(fine.outstanding()).isEqualTo(rm("54.00"));
    }

    @ParameterizedTest(name = "half of {0} sen is at most {1} sen")
    @CsvSource({"8000, 4000", "1, 0", "3, 1", "101, 50"})
    void theCapRoundsInTheProvidersFavour(long grossSen, long mostSen) {
        assertThat(FineDeductionPolicy.mostDeductible(Money.ofSen(grossSen), HALF)).isEqualTo(Money.ofSen(mostSen));
    }

    @Test
    void finesAreDeductedFromAPayoutOnce() {
        Payout payout = aPayoutOf("80.00");
        FineDeductionPolicy.apply(payout, List.of(), HALF);

        assertThatThrownBy(() -> FineDeductionPolicy.apply(payout, List.of(), HALF)).isInstanceOf(PayoutAlreadyDeductedException.class);
    }

    @Test
    void aProvidersPayoutPaysOnlyTheirOwnFines() {
        Fine someoneElses = Fine.charge("provider-2", new FineCharge("reservation-0", rm("10"), FineReason.NO_SHOW), monday());

        assertThatThrownBy(() -> FineDeductionPolicy.apply(aPayoutOf("80.00"), List.of(someoneElses), HALF))
                .isInstanceOf(FineOfAnotherProviderException.class);
        assertThat(someoneElses.deducted()).isEqualTo(rm("0.00"));
    }
}
