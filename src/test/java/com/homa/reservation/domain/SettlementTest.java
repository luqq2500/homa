package com.homa.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.reservation.domain.settlement.Settlement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SettlementTest {

    private static final Money ZERO = Money.myr(0);

    @Test
    @Tag("CAN-2")
    @DisplayName("For a RM25 job, a no-show fine of 25 + 30% × 25 = RM32.50 is capped at RM25.")
    void providerFineIsCappedAtTheTotalReservationFee() {
        Settlement settlement = Settlement.of(Money.myr(25), Money.myr(25), ZERO, ZERO, Money.myr("32.50"));

        assertThat(settlement.providerFine()).isEqualTo(Money.myr(25));
        assertThat(settlement.customerFee()).isEqualTo(ZERO);
        assertThat(settlement.customerRefund()).isEqualTo(Money.myr(25));
    }

    @Test
    @Tag("CAN-2")
    @DisplayName("No customer fee is more than the Total Reservation Fee: a RM30 fee on a RM25 payment is RM25, and nothing comes back.")
    void customerFeeIsCappedAtTheTotalReservationFeeAndTheProviderIsOwedTheCappedFee() {
        Settlement settlement = Settlement.of(Money.myr(25), Money.myr(25), Money.myr(30), ZERO, ZERO);

        assertThat(settlement.customerFee()).isEqualTo(Money.myr(25));
        assertThat(settlement.customerRefund()).isEqualTo(ZERO);
        assertThat(settlement.providerPayoutGross()).isEqualTo(Money.myr(25));
    }

    @Test
    @Tag("CAN-2")
    @DisplayName("A fee or fine up to the cap is kept as it is, and refund + fee is the amount held.")
    void aFeeAndFineWithinTheCapAreKept() {
        Settlement atTheCap = Settlement.of(Money.myr(200), Money.myr(200), Money.myr(200), ZERO, Money.myr(200));
        Settlement belowTheCap = Settlement.of(Money.myr(200), Money.myr(200), Money.myr(45), Money.myr(6),
                Money.myr(65));

        assertThat(atTheCap.customerFee()).isEqualTo(Money.myr(200));
        assertThat(atTheCap.providerFine()).isEqualTo(Money.myr(200));
        assertThat(atTheCap.customerRefund()).isEqualTo(ZERO);
        assertThat(belowTheCap).isEqualTo(new Settlement(Money.myr(45), Money.myr(155), Money.myr(6), Money.myr(65),
                Money.myr(45)));
    }

    @Test
    @Tag("CAN-2")
    @DisplayName("The cap is a share of the Total Reservation Fee Homa sets, which the reservation was Requested with.")
    void theCapIsTheOneGiven() {
        Settlement settlement = Settlement.of(Money.myr(200), Money.myr(100), Money.myr(150), ZERO, Money.myr(120));

        assertThat(settlement.customerFee()).isEqualTo(Money.myr(100));
        assertThat(settlement.providerFine()).isEqualTo(Money.myr(100));
        assertThat(settlement.customerRefund()).isEqualTo(Money.myr(100));
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("Each money line is rounded once to the nearest sen, half up, and a refund is the amount paid minus the rounded fee.")
    void everyLineIsRoundedOnceHalfUpAndTheRefundIsTheAmountPaidMinusTheRoundedFee() {
        Settlement settlement = Settlement.of(Money.myr(200), Money.myr(200), Money.myr("160.625"),
                Money.myr("5.625"), Money.myr("0.005"));

        assertThat(settlement.customerFee().amount()).isEqualByComparingTo("160.63");
        assertThat(settlement.customerFee().amount().scale()).isEqualTo(2);
        assertThat(settlement.customerRefund().amount()).isEqualByComparingTo("39.37");
        assertThat(settlement.compensation().amount()).isEqualByComparingTo("5.63");
        assertThat(settlement.compensation().amount().scale()).isEqualTo(2);
        assertThat(settlement.providerFine().amount()).isEqualByComparingTo("0.01");
        assertThat(settlement.providerFine().amount().scale()).isEqualTo(2);
        assertThat(settlement.providerPayoutGross()).isEqualTo(settlement.customerFee());
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A line that rounds down stays down: RM0.004 is no sen.")
    void aLineBelowHalfASenRoundsDown() {
        Settlement settlement = Settlement.of(Money.myr(200), Money.myr(200), Money.myr("0.004"),
                Money.myr("0.004"), Money.myr("0.004"));

        assertThat(settlement.customerFee().amount()).isEqualByComparingTo("0.00");
        assertThat(settlement.compensation().amount()).isEqualByComparingTo("0.00");
        assertThat(settlement.providerFine().amount()).isEqualByComparingTo("0.00");
        assertThat(settlement.customerRefund()).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("CAN-2")
    @DisplayName("Persistence: a settlement comes back with its five lines, and each line is needed.")
    void aStoredSettlementNeedsAllItsLines() {
        Money line = Money.myr(1);

        assertThat(new Settlement(line, line, line, line, line).providerFine()).isEqualTo(line);
        assertThatThrownBy(() -> new Settlement(null, line, line, line, line)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Settlement(line, null, line, line, line)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Settlement(line, line, null, line, line)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Settlement(line, line, line, null, line)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Settlement(line, line, line, line, null)).isInstanceOf(NullPointerException.class);
    }
}
