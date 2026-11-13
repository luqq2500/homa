package payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static payment.domain.PaymentBuilders.rm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import payment.domain.exception.PayoutNotHeldException;
import payment.domain.exception.PayoutNotPayableException;
import payment.domain.exception.PayoutReasonRequiredException;

/** Feature: A seriously flagged provider's payouts are held for the admins' review. */
class PayoutHoldTest {

    private static Payout aPayoutToKemas() {
        return Payout.create("provider-1", "reservation-1", rm("200.00"), PayoutKind.COMPLETED_JOB);
    }

    @Test
    void aNewPayoutIsPayable() {
        Payout payout = aPayoutToKemas();

        assertThat(payout.status()).isEqualTo(PayoutStatus.PAYABLE);
        assertThat(payout.isPayable()).isTrue();
        assertThat(payout.reason()).isEmpty();
    }

    @Test
    void aFlaggedProvidersPayoutIsHeld() {
        Payout payout = aPayoutToKemas();

        payout.hold(" Struck off the SSM register ");

        assertThat(payout.status()).isEqualTo(PayoutStatus.HELD);
        assertThat(payout.isPayable()).isFalse();
        assertThat(payout.reason()).contains("Struck off the SSM register");
    }

    @Test
    void theAdminsReleaseAHeldPayout() {
        Payout payout = aPayoutToKemas();
        payout.hold("Possible sanctions match");

        payout.releaseHold();

        assertThat(payout.isPayable()).isTrue();
        assertThat(payout.reason()).isEmpty();
    }

    @Test
    void theAdminsWithholdAHeldPayout() {
        Payout payout = aPayoutToKemas();
        payout.hold("Sanctions match");

        payout.withhold("Confirmed sanctions match");

        assertThat(payout.status()).isEqualTo(PayoutStatus.WITHHELD);
        assertThat(payout.isPayable()).isFalse();
        assertThat(payout.reason()).contains("Confirmed sanctions match");
    }

    @Test
    void onlyAPayablePayoutIsHeld() {
        Payout payout = aPayoutToKemas();
        payout.hold("Sanctions match");

        assertThatThrownBy(() -> payout.hold("Again"))
                .isInstanceOf(PayoutNotPayableException.class)
                .hasMessageContaining(payout.id())
                .hasMessageContaining("HELD");
    }

    @Test
    void onlyAHeldPayoutIsReleasedOrWithheld() {
        Payout payable = aPayoutToKemas();
        Payout withheld = aPayoutToKemas();
        withheld.hold("Sanctions match");
        withheld.withhold("Confirmed sanctions match");

        assertThatThrownBy(payable::releaseHold).isInstanceOf(PayoutNotHeldException.class).hasMessageContaining("PAYABLE");
        assertThatThrownBy(() -> payable.withhold("Sanctions")).isInstanceOf(PayoutNotHeldException.class);
        assertThatThrownBy(withheld::releaseHold).isInstanceOf(PayoutNotHeldException.class).hasMessageContaining("WITHHELD");
        assertThat(withheld.status()).isEqualTo(PayoutStatus.WITHHELD);
    }

    @ParameterizedTest(name = "the reason \"{0}\" is not enough")
    @NullSource
    @ValueSource(strings = {"", " "})
    void holdingAndWithholdingNeedAReason(String reason) {
        Payout payout = aPayoutToKemas();

        assertThatThrownBy(() -> payout.hold(reason)).isInstanceOf(PayoutReasonRequiredException.class);
        payout.hold("Sanctions match");
        assertThatThrownBy(() -> payout.withhold(reason)).isInstanceOf(PayoutReasonRequiredException.class);
        assertThat(payout.status()).isEqualTo(PayoutStatus.HELD);
    }
}
