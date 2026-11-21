package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.BASE_CANCELLATION_FEE;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HELPERS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HOURS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_RATE;
import static com.homa.fixtures.HomaFixtures.FLAT_TRAVEL_FEE;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.fixtures.HomaFixtures.HOURS_FEE;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.aisyahAddress;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.pricing.ReservationPricing;
import com.homa.reservation.domain.replica.TravelFeeRule;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import com.homa.reservation.domain.terms.CancellationTier;
import com.homa.reservation.domain.terms.DiscountRule;
import com.homa.reservation.domain.terms.ReservationTerms;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReservationPricingTest {

    private static final ReservationTerms KEMAS_TERMS = new ReservationTerms(HOURLY_RATE_PER_HELPER, FLAT_TRAVEL_FEE,
            new DiscountRule(DISCOUNT_RATE, DISCOUNT_MIN_HOURS, DISCOUNT_MIN_HELPERS), AcceptancePolicy.REQUEST,
            CancellationTier.STANDARD);

    @Test
    @Tag("RES-6")
    @DisplayName("Standard reservation: 25 × 4 × 2 × 0.9 + 20 = RM200.")
    void totalReservationFeeIsHoursFeePlusTravelFee() {
        Quote quote = quote(240, 2);

        assertThat(quote.hoursFee()).isEqualTo(HOURS_FEE);
        assertThat(quote.travelFee()).isEqualTo(FLAT_TRAVEL_FEE);
        assertThat(quote.total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(quote.discountRate()).isEqualTo(DISCOUNT_RATE);
    }

    @Test
    @DisplayName("The Base Cancellation Fee is one discounted hour for both helpers: 25 × 2 × 1 × 0.9 = RM45.")
    void baseCancellationFeeIsOneDiscountedHourForAllBookedHelpers() {
        assertThat(quote(240, 2).baseCancellationFee()).isEqualTo(BASE_CANCELLATION_FEE);
    }

    @Test
    @Tag("RES-6")
    @DisplayName("A 2-hour, 2-helper booking doesn't get the discount: 25 × 2 × 2 + 20 = RM120.")
    void discountAppliesOnlyFromTheMinimumHoursAndHelpers() {
        assertThat(quote(120, 2).total()).isEqualTo(Money.myr(120));
        assertThat(quote(120, 2).discountRate()).isEqualTo(Percentage.ZERO);
        assertThat(quote(165, 2).total()).isEqualTo(Money.myr("157.50"));
        assertThat(quote(180, 2).total()).isEqualTo(Money.myr(155));
        assertThat(quote(240, 1).total()).isEqualTo(Money.myr(120));
    }

    @Test
    @Tag("RES-6")
    @DisplayName("3h 15m with 3 helpers: 25 × 3.25 × 3 × 0.9 = RM219.375 exactly, and the total is rounded once to RM239.38.")
    void totalIsRoundedOnceToTheSen() {
        Quote quote = quote(195, 3);

        assertThat(quote.hoursFee()).isEqualTo(Money.myr("219.375"));
        assertThat(quote.total()).isEqualTo(Money.myr("239.38"));
        assertThat(quote.baseCancellationFee()).isEqualTo(Money.myr("67.5"));
    }

    @Test
    @Tag("RES-6")
    @DisplayName("Kemas Home sets RM20 flat. Another provider sets RM10 for Salak Tinggi and RM25 for Putrajaya.")
    void travelFeeFollowsTheProvidersTravelFeeRule() {
        assertThat(TravelFeeRule.flat(FLAT_TRAVEL_FEE).feeFor("PUTRAJAYA")).isEqualTo(FLAT_TRAVEL_FEE);
        assertThat(TravelFeeRule.none().feeFor("PUTRAJAYA")).isEqualTo(Money.myr(0));
        TravelFeeRule byCity = TravelFeeRule.byCity(Map.of("SALAK_TINGGI", Money.myr(10), "PUTRAJAYA", Money.myr(25)));
        assertThat(byCity.feeFor("SALAK_TINGGI")).isEqualTo(Money.myr(10));
        assertThat(byCity.feeFor("PUTRAJAYA")).isEqualTo(Money.myr(25));
        assertRefused(() -> byCity.feeFor("SHAH_ALAM"), BookingRefusal.CITY_NOT_COVERED);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("The terms frozen for a reservation carry the travel fee of its city.")
    void termsAreFrozenWithTheTravelFeeOfTheReservationsCity() {
        ReservationTerms terms = kemasHomeProvider()
                .travelFee(TravelFeeRule.byCity(Map.of("SALAK_TINGGI", Money.myr(10))))
                .build()
                .termsFor(aisyahAddress());

        assertThat(terms).isEqualTo(new ReservationTerms(HOURLY_RATE_PER_HELPER, Money.myr(10),
                new DiscountRule(DISCOUNT_RATE, DISCOUNT_MIN_HOURS, DISCOUNT_MIN_HELPERS), AcceptancePolicy.REQUEST,
                CancellationTier.STANDARD));
    }

    @Test
    @DisplayName("No discount by default.")
    void noDiscountByDefault() {
        assertThat(DiscountRule.NONE.rateFor(JobDuration.ofMinutes(480), 3)).isEqualTo(Percentage.ZERO);
    }

    private static Quote quote(long minutes, int helpers) {
        return ReservationPricing.quote(KEMAS_TERMS, JobDuration.ofMinutes(minutes), helpers);
    }
}
