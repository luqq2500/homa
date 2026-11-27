package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.reservation.usecase.dto.QuoteReservationRequest;
import com.homa.reservation.usecase.dto.QuoteReservationResponse;
import com.homa.reservation.usecase.exception.CustomerNotFoundException;
import com.homa.reservation.usecase.exception.ProviderNotFoundException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class QuoteReservationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("RES-6")
    @DisplayName("25 × 4 × 2 × 0.9 + 20 = RM200.")
    void standardReservationCostsTwoHundred() {
        QuoteReservationResponse quote = bed.quoteReservation.quote(
                new QuoteReservationRequest(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, 240, 2));

        assertThat(quote).isEqualTo(new QuoteReservationResponse(Money.myr(180), Money.myr(20), Money.myr(200),
                Money.myr(45), BigDecimal.valueOf(10)));
    }

    @Test
    @Tag("OFR-4")
    @DisplayName("2 hours with 2 helpers is below the 3-hour minimum, so no discount applies: 25 × 2 × 2 + 20 = RM120.")
    void shorterBookingGetsNoDiscount() {
        QuoteReservationResponse quote = bed.quoteReservation.quote(
                new QuoteReservationRequest(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, 120, 2));

        assertThat(quote.total()).isEqualTo(Money.myr(120));
        assertThat(quote.baseCancellationFee()).isEqualTo(Money.myr(50));
        assertThat(quote.discountPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @Tag("RES-6")
    @DisplayName("A travel fee by city is quoted for the city of the customer's home.")
    void travelFeeIsTheOneForTheCustomersCity() {
        ReservationTestBed.ListingEvent byCity = bed.kemasListing();
        byCity.travelFeeKind = "BY_CITY";
        byCity.travelFeeByCity = Map.of("SALAK_TINGGI", Money.myr(15), "PUTRAJAYA", Money.myr(30));
        bed.listingTermsChanged.handle(byCity.build(bed.nextInboundId()));

        QuoteReservationResponse quote = bed.quoteReservation.quote(
                new QuoteReservationRequest(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, 240, 2));

        assertThat(quote.travelFee()).isEqualTo(Money.myr(15));
        assertThat(quote.total()).isEqualTo(Money.myr(195));
    }

    @Test
    @Tag("RES-6")
    @DisplayName("A quote needs a registered customer and a listed provider.")
    void unknownCustomerOrProviderIsNotFound() {
        assertThatThrownBy(() -> bed.quoteReservation.quote(
                new QuoteReservationRequest("customer-unknown", KEMAS_HOME_PROVIDER_ID, 240, 2)))
                .isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> bed.quoteReservation.quote(
                new QuoteReservationRequest(AISYAH_CUSTOMER_ID, "provider-unknown", 240, 2)))
                .isInstanceOf(ProviderNotFoundException.class);
    }
}
