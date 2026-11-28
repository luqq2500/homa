package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.reservation.domain.ReservationFixtures.friday;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.Money;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.settlement.FreeCancellationPolicy;
import com.homa.reservation.domain.settlement.Settlement;
import com.homa.reservation.domain.settlement.SettlementBasis;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The money regression gate: §12 "Summary of money outcomes" for the cases the request and cancellation slices
 * cover, one row per case, on the standard reservation (Total RM200, Base RM45). Each row runs the reservation's own
 * behaviour at a moment, or the free outcome of a request that ends, and checks every line Payment executes.
 */
class SettlementOutcomesTest {

    /** One row of §12: what the customer pays and receives, what the provider is fined, and what the provider is owed. */
    private record Row(String customerPays, String providerFine, String customerReceives, String providerOwed) {

        Money pays() {
            return Money.myr(customerPays);
        }

        Money fine() {
            return Money.myr(providerFine);
        }

        /** The refund and the compensation on top of it, as the table's "Customer receives" column adds them. */
        Money receives() {
            return Money.myr(customerReceives);
        }

        Money owed() {
            return Money.myr(providerOwed);
        }
    }

    private static Arguments row(String name, Function<Reservation, Settlement> outcome, Row row) {
        return Arguments.of(name, outcome, row);
    }

    /** A request that ends free (§12 rows 2 and 3): the customer's whole payment goes back. */
    private static Function<Reservation, Settlement> freeOutcomeAt(Instant at) {
        return requested -> new FreeCancellationPolicy()
                .settle(new SettlementBasis(requested.quote(), requested.jobTime(), requested.values()), at);
    }

    private static Function<Reservation, Settlement> customerCancelsAt(Instant at) {
        return reserved -> reserved.cancelByCustomer(at).settlement();
    }

    private static Function<Reservation, Settlement> providerCancelsAt(Instant at) {
        return reserved -> reserved.cancelByProvider(at).settlement();
    }

    /** A reservation whose roster isn't complete when the Check-in Period starts (§12 "No roster"). */
    private static Function<Reservation, Settlement> noRosterAt(Instant at) {
        return reserved -> reserved.lockRosterAtCheckIn(at).orElseThrow().settlement();
    }

    static Stream<Arguments> everyCaseOfTheMoneyTable() {
        return Stream.of(
                row("Customer withdraws a request: full refund, gets RM200", freeOutcomeAt(monday(21, 0)),
                        new Row("0", "0", "200", "0")),
                row("Provider declines, or the request expires: full refund, gets RM200",
                        freeOutcomeAt(at(2026, 12, 1, 20, 0)), new Row("0", "0", "200", "0")),
                row("Customer cancels in the Grace Period: full refund, gets RM200", customerCancelsAt(monday(23, 30)),
                        new Row("0", "0", "200", "0")),
                row("Customer cancels before the cut-off: full refund, gets RM200", customerCancelsAt(friday(21, 44)),
                        new Row("0", "0", "200", "0")),
                row("Customer cancels after the cut-off: pays RM45, gets RM155", customerCancelsAt(friday(22, 0)),
                        new Row("45", "0", "155", "45")),
                row("Provider cancels, more than 24h ahead: fine RM45, gets RM200",
                        providerCancelsAt(at(2026, 12, 2, 10, 0)), new Row("0", "45", "200", "0")),
                row("Provider cancels, within 24h: fine RM65, gets RM206", providerCancelsAt(friday(20, 0)),
                        new Row("0", "65", "206", "0")),
                row("Customer cancels last minute at 9:50: pays RM65, gets RM135", customerCancelsAt(saturday(9, 50)),
                        new Row("65", "0", "135", "65")),
                row("Customer cancels last minute at 11:20: pays RM87.50, gets RM112.50",
                        customerCancelsAt(saturday(11, 20)), new Row("87.50", "0", "112.50", "87.50")),
                row("Provider cancels last minute: fine RM85, gets RM214", providerCancelsAt(saturday(9, 50)),
                        new Row("0", "85", "214", "0")),
                row("No roster at 9:45: fine RM69, gets RM208", noRosterAt(saturday(9, 45)),
                        new Row("0", "69", "208", "0")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyCaseOfTheMoneyTable")
    @Tag("REQ-2")
    @Tag("REQ-3")
    @Tag("REQ-4")
    @Tag("CCX-1")
    @Tag("CCX-6")
    @Tag("PCX-1")
    @Tag("PCX-2")
    @Tag("LMC-1")
    @Tag("LMC-2")
    @Tag("LMC-3")
    @Tag("ROS-7")
    @Tag("NRS-1")
    @Tag("NRS-2")
    @Tag("NRS-3")
    @Tag("CAN-1")
    @Tag("CAN-3")
    @DisplayName("§12 Summary of money outcomes: Aisyah's RM200 reservation, with a Base Cancellation Fee of RM45, settles as the table says in every case.")
    void everyCaseSettlesAsTheMoneyTableSays(String name, Function<Reservation, Settlement> outcome, Row row) {
        Reservation reserved = Marketplace.withKemasHome().reserveStandard();

        Settlement settlement = outcome.apply(reserved);

        assertThat(settlement.customerFee()).as("customer pays").isEqualTo(row.pays());
        assertThat(settlement.providerFine()).as("provider fine").isEqualTo(row.fine());
        assertThat(settlement.customerRefund().plus(settlement.compensation())).as("customer receives")
                .isEqualTo(row.receives());
        assertThat(settlement.providerPayoutGross()).as("provider owed").isEqualTo(row.owed());
        assertThat(settlement.customerFee().plus(settlement.customerRefund())).as("refund + fee is the amount held")
                .isEqualTo(Money.myr(200));
    }
}
