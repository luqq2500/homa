package payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static payment.domain.PaymentBuilders.NOW;
import static payment.domain.PaymentBuilders.aFineOf;
import static payment.domain.PaymentBuilders.aPayoutOf;
import static payment.domain.PaymentBuilders.monday;
import static payment.domain.PaymentBuilders.rm;

import java.util.List;
import kernel.Percentage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import payment.domain.exception.FineAlreadyClearedException;
import payment.domain.exception.FineAlreadyWaivedException;
import payment.domain.exception.InvalidFineAmountException;
import payment.domain.exception.InvalidPayoutAmountException;
import payment.domain.exception.PaymentDetailRequiredException;
import payment.domain.exception.WaiverReasonRequiredException;

/** Fines and payouts on their own. */
class FineAndPayoutTest {

    @Nested
    class Fines {

        @Test
        void aChargedFineIsOwedInFull() {
            Fine fine = Fine.charge("provider-1", new FineCharge("reservation-7", rm("54.00"), FineReason.NO_SHOW), monday());

            assertThat(fine.outstanding()).isEqualTo(rm("54.00"));
            assertThat(fine.amount()).isEqualTo(rm("54.00"));
            assertThat(fine.reason()).isEqualTo(FineReason.NO_SHOW);
            assertThat(fine.reservationId()).isEqualTo("reservation-7");
            assertThat(fine.providerId()).isEqualTo("provider-1");
            assertThat(fine.chargedAt()).isEqualTo(monday());
            assertThat(fine.id()).isNotBlank();
            assertThat(fine.isWaived()).isFalse();
            assertThat(fine.isCleared()).isFalse();
            assertThat(fine.waiverReason()).isEmpty();
            assertThat(fine.waivedAt()).isEmpty();
        }

        @Test
        void aFineIsAboveZeroAndBelongsToAProvider() {
            assertThatThrownBy(() -> Fine.charge("provider-1", new FineCharge("r", rm("0"), FineReason.NO_SHOW), monday()))
                    .isInstanceOf(InvalidFineAmountException.class);
            assertThatThrownBy(() -> Fine.charge("", new FineCharge("r", rm("10"), FineReason.NO_SHOW), monday()))
                    .isInstanceOf(PaymentDetailRequiredException.class);
            assertThatThrownBy(() -> Fine.charge("provider-1", new FineCharge("r", rm("10"), FineReason.NO_SHOW), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void aPartlyDeductedFineCanBeWaived() {
            Fine fine = aFineOf("54.00", monday());
            FineDeductionPolicy.apply(aPayoutOf("80.00"), List.of(fine), Percentage.of(50));

            fine.waive(" Customer caused the problem ", NOW);

            assertThat(fine.outstanding()).isEqualTo(rm("0.00"));
            assertThat(fine.deducted()).isEqualTo(rm("40.00"));
            assertThat(fine.waiverReason()).contains("Customer caused the problem");
            assertThat(fine.waivedAt()).contains(NOW);
        }

        @Test
        void aFineIsWaivedOnce() {
            Fine fine = aFineOf("36.00", monday());
            fine.waive("Genuine emergency", NOW);

            assertThatThrownBy(() -> fine.waive("Again", NOW)).isInstanceOf(FineAlreadyWaivedException.class);
        }

        @Test
        void aClearedFineHasNothingToWaive() {
            Fine fine = aFineOf("14.00", monday());
            FineDeductionPolicy.apply(aPayoutOf("100.00"), List.of(fine), Percentage.of(50));

            assertThatThrownBy(() -> fine.waive("Too late", NOW)).isInstanceOf(FineAlreadyClearedException.class);
        }

        @Test
        void aWaiverNeedsAReason() {
            Fine fine = aFineOf("36.00", monday());

            assertThatThrownBy(() -> fine.waive(" ", NOW)).isInstanceOf(WaiverReasonRequiredException.class);
            assertThatThrownBy(() -> fine.waive(null, NOW)).isInstanceOf(WaiverReasonRequiredException.class);
            assertThat(fine.isWaived()).isFalse();
        }
    }

    @Nested
    class Payouts {

        @Test
        void aPayoutPaysOneReservationsEarning() {
            Payout payout = aPayoutOf("200.00");

            assertThat(payout.gross()).isEqualTo(rm("200.00"));
            assertThat(payout.net()).isEqualTo(rm("200.00"));
            assertThat(payout.providerId()).isEqualTo("provider-1");
            assertThat(payout.reservationId()).isEqualTo("reservation-1");
            assertThat(payout.kind()).isEqualTo(PayoutKind.COMPLETED_JOB);
            assertThat(payout.id()).isNotBlank();
        }

        @Test
        void aPayoutPaysSomething() {
            assertThatThrownBy(() -> Payout.create("provider-1", "reservation-1", rm("0"), PayoutKind.COMPLETED_JOB))
                    .isInstanceOf(InvalidPayoutAmountException.class);
            assertThatThrownBy(() -> Payout.create("provider-1", " ", rm("1"), PayoutKind.COMPLETED_JOB))
                    .isInstanceOf(PaymentDetailRequiredException.class);
            assertThatThrownBy(() -> Payout.create(null, "reservation-1", rm("1"), PayoutKind.COMPLETED_JOB))
                    .isInstanceOf(PaymentDetailRequiredException.class);
            assertThatThrownBy(() -> Payout.create("provider-1", "reservation-1", rm("1"), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
