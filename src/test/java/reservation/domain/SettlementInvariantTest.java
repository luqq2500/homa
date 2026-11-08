package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

/** Money invariants: every ending adds up to the total, and no cancellation fee is above the no-show fee. */
class SettlementInvariantTest {

    private static List<Settlement> everyEndingOn(CancellationTier tier, int hours, String rate) {
        List<Settlement> settlements = new ArrayList<>();
        for (Instant at : List.of(thursday("12:00"), friday("09:59"), friday("10:00"), saturday("09:59"))) {
            settlements.add(onTier(tier, hours, rate).reserved().cancelByCustomer(at));
            settlements.add(onTier(tier, hours, rate).reserved().cancelByProvider(at).settlement());
        }
        settlements.add(onTier(tier, hours, rate).requested().cancelByCustomer(thursday("12:00")));
        settlements.add(onTier(tier, hours, rate).checkedInAt(saturday("10:00")).cancelByCustomer(saturday("10:20")));
        settlements.add(onTier(tier, hours, rate).checkedInAt(saturday("10:00")).reportCustomerNoShow(saturday("10:15")));
        settlements.add(onTier(tier, hours, rate).reservedWithTeamNamed().markProviderNoShow(saturday("10:16")).settlement());
        return settlements;
    }

    private static ReservationBuilders.ReservationBuilder onTier(CancellationTier tier, int hours, String rate) {
        return aReservation()
                .withOffering(ReservationBuilders.anOffering().charging(rate).withSeparateTravelFee("17.35"))
                .onTier(tier)
                .forBooking(aBooking().forHours(hours));
    }

    @ParameterizedTest(name = "on the {0} tier every ending adds up to the total")
    @EnumSource(CancellationTier.class)
    void everyEndingAddsUpToTheTotal(CancellationTier tier) {
        for (int hours = 2; hours <= 4; hours++) {
            for (String rate : List.of("30", "33.33", "35.55")) {
                for (Settlement settlement : everyEndingOn(tier, hours, rate)) {
                    assertThat(settlement.providerEarning().plus(settlement.serviceFee()).plus(settlement.customerRefund()))
                            .isEqualTo(settlement.total());
                    assertThat(settlement.customerRefund().isNegative()).isFalse();
                }
            }
        }
    }

    @ParameterizedTest(name = "the {0} cancellation fee is at most the customer no-show fee")
    @EnumSource(CancellationTier.class)
    void theNoShowFeeIsTheCeiling(CancellationTier tier) {
        Settlement latestCancellation = aReservation().onTier(tier).reserved().cancelByCustomer(saturday("09:59"));
        Settlement noShow = aReservation().onTier(tier).checkedInAt(saturday("10:00")).reportCustomerNoShow(saturday("10:15"));

        assertThat(latestCancellation.providerEarning()).isLessThanOrEqualTo(noShow.providerEarning());
    }

    @Test
    void aSettlementBelongsToAReservationAndHasAnEndingAndARelease() {
        assertThatThrownBy(() -> new Settlement(null, Ending.COMPLETED, rm("1"), rm("1"), rm("0"), rm("0"), saturday("10:00")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Settlement("r", null, rm("1"), rm("1"), rm("0"), rm("0"), saturday("10:00")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Settlement("r", Ending.COMPLETED, rm("1"), rm("1"), rm("0"), rm("0"), null))
                .isInstanceOf(NullPointerException.class);
    }
}
