package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Percentage;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PaymentValuesTest {

    private static final PaymentValues.Version FIRST = PaymentValues.current();
    /** A made-up later change: from Tuesday 1 December 2026, an 18% platform fee and a 25% deduction cap. */
    private static final Instant CHANGED_FROM = at(2026, 12, 1, 0, 0);
    private static final PaymentValues.Version CHANGED =
            new PaymentValues.Version(CHANGED_FROM, Percentage.of(18), Percentage.of(25));

    @Test
    @Tag("PAY-3")
    @Tag("PAY-5")
    @DisplayName("Homa-set values: the platform fee is 15.5% of every payout and the fine deduction cap is 20% of "
            + "each payout.")
    void paymentValuesAreThePlatformFeeAndTheFineDeductionCap() {
        PaymentValues.Version current = PaymentValues.current();

        assertThat(current.effectiveFrom()).isEqualTo(Instant.EPOCH);
        assertThat(current.platformFeeRate()).isEqualTo(Percentage.of("15.5"));
        assertThat(current.fineDeductionCap()).isEqualTo(Percentage.of(20));
        assertThat(PaymentValues.asOf(REQUESTED_AT)).isEqualTo(current);
        assertThat(PaymentValues.homa().inForceAt(REQUESTED_AT)).isEqualTo(current);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Homa changes its values from Tuesday: Aisyah's reservation, Requested on Monday 20:00, keeps the "
            + "values from then; a reservation Requested from Tuesday 00:00 gets the new ones.")
    void reservationKeepsTheVersionInForceWhenItWasRequested() {
        PaymentValues values = new PaymentValues(List.of(FIRST, CHANGED));

        assertThat(values.inForceAt(REQUESTED_AT)).isEqualTo(FIRST);
        assertThat(values.inForceAt(CHANGED_FROM.minusNanos(1))).isEqualTo(FIRST);
        assertThat(values.inForceAt(CHANGED_FROM)).isEqualTo(CHANGED);
        assertThat(values.inForceAt(at(2027, 1, 1, 0, 0))).isEqualTo(CHANGED);
        assertThat(values.newest()).isEqualTo(CHANGED);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Payment's values start with a version in force from the epoch, each later one strictly after.")
    void versionsStartAtTheEpochAndFollowInOrder() {
        PaymentValues.Version notAtEpoch = new PaymentValues.Version(CHANGED_FROM, Percentage.of(18), Percentage.of(20));
        PaymentValues.Version sameStart = new PaymentValues.Version(Instant.EPOCH, Percentage.of(18), Percentage.of(20));

        assertThatThrownBy(() -> new PaymentValues(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PaymentValues(List.of(notAtEpoch)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("epoch");
        assertThatThrownBy(() -> new PaymentValues(List.of(FIRST, sameStart)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1970-01-01T00:00:00Z");
        assertThatThrownBy(() -> new PaymentValues(List.of(FIRST, CHANGED, FIRST)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new PaymentValues(List.of(FIRST, CHANGED)).newest()).isEqualTo(CHANGED);
    }
}
