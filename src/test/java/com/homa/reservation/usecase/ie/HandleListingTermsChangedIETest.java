package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HELPERS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HOURS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_RATE;
import static com.homa.fixtures.HomaFixtures.FLAT_TRAVEL_FEE;
import static com.homa.fixtures.HomaFixtures.JOB_BUFFER;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_CITIES;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.MINIMUM_NOTICE;
import static com.homa.reservation.usecase.ReservationTestBed.BATHROOM_CLEANING;
import static com.homa.reservation.usecase.ReservationTestBed.KITCHEN_CLEANING;
import static com.homa.reservation.usecase.ReservationTestBed.LAUNDRY_FOLDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.replica.BookingBounds;
import com.homa.reservation.domain.replica.OfferedTerms;
import com.homa.reservation.domain.replica.ProviderListing;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.domain.replica.TravelFeeRule;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import com.homa.reservation.domain.terms.CancellationTier;
import com.homa.reservation.domain.terms.DiscountRule;
import com.homa.reservation.usecase.ReservationTestBed;
import com.homa.reservation.usecase.ReservationTestBed.ListingEvent;
import com.homa.reservation.usecase.dto.QuoteReservationRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleListingTermsChangedIETest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private ProviderListing listing() {
        return bed.providers.findById(KEMAS_HOME_PROVIDER_ID).flatMap(ProviderReplica::listing).orElseThrow();
    }

    private Money quotedTotal() {
        return bed.quoteReservation.quote(
                new QuoteReservationRequest(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, 240, 2)).total();
    }

    @Test
    @Tag("OFR-9")
    @DisplayName("Kemas Home's listing is kept as Listing published it: cities, jobs, terms, bounds, notice and buffer.")
    void listingIsKeptFieldByField() {
        assertThat(listing()).isEqualTo(new ProviderListing(
                Set.copyOf(KEMAS_HOME_CITIES),
                Set.of(KITCHEN_CLEANING, LAUNDRY_FOLDING, BATHROOM_CLEANING),
                new OfferedTerms(Money.myr(25), TravelFeeRule.flat(FLAT_TRAVEL_FEE),
                        new DiscountRule(DISCOUNT_RATE, DISCOUNT_MIN_HOURS, DISCOUNT_MIN_HELPERS),
                        AcceptancePolicy.REQUEST, CancellationTier.STANDARD),
                new BookingBounds(Duration.ofHours(2), Duration.ofHours(6), Duration.ofMinutes(15), 1, 6),
                MINIMUM_NOTICE,
                JOB_BUFFER));
        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow().bookable()).isTrue();
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Kemas Home raises its rate to RM30 and switches to Firm on Wednesday; Aisyah's Saturday booking stays RM200 under Standard.")
    void newTermsApplyOnlyToNewBookings() {
        Reservation booked = bed.requestStandard();
        ListingEvent raised = bed.kemasListing();
        raised.rate = Money.myr(30);
        raised.cancellationTier = "FIRM";

        bed.listingTermsChanged.handle(raised.build(bed.nextInboundId()));

        Reservation kept = bed.reload(booked);
        assertThat(kept.quote().total()).isEqualTo(Money.myr(200));
        assertThat(kept.terms().cancellationTier()).isEqualTo(CancellationTier.STANDARD);
        assertThat(listing().terms().cancellationTier()).isEqualTo(CancellationTier.FIRM);
        assertThat(quotedTotal()).isEqualTo(Money.myr(236));
    }

    @Test
    @Tag("OFR-3")
    @DisplayName("The travel fee is none, flat, or by city; any other kind is refused and the event held.")
    void travelFeeIsNoneFlatOrByCity() {
        ListingEvent none = bed.kemasListing();
        none.travelFeeKind = "NONE";
        none.flatTravelFee = null;
        bed.listingTermsChanged.handle(none.build(bed.nextInboundId()));
        assertThat(listing().terms().travelFee()).isEqualTo(TravelFeeRule.none());

        ListingEvent byCity = bed.kemasListing();
        byCity.travelFeeKind = "BY_CITY";
        byCity.travelFeeByCity = Map.of("SALAK_TINGGI", Money.myr(15));
        bed.listingTermsChanged.handle(byCity.build(bed.nextInboundId()));
        assertThat(listing().terms().travelFee()).isEqualTo(TravelFeeRule.byCity(Map.of("SALAK_TINGGI", Money.myr(15))));

        ListingEvent unknown = bed.kemasListing();
        unknown.travelFeeKind = "PER_KILOMETRE";
        ListingTermsChangedIE unreadable = unknown.build(bed.nextInboundId());
        assertThatThrownBy(() -> bed.listingTermsChanged.handle(unreadable))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(listing().terms().travelFee()).isEqualTo(TravelFeeRule.byCity(Map.of("SALAK_TINGGI", Money.myr(15))));
    }

    @Test
    @Tag("OFR-4")
    @DisplayName("A listing with no discount has none: 25 × 4 × 2 + 20 = RM220.")
    void listingWithoutADiscountHasNone() {
        ListingEvent noDiscount = bed.kemasListing();
        noDiscount.discountRate = null;
        noDiscount.discountMinHours = null;
        noDiscount.discountMinHelpers = 0;

        bed.listingTermsChanged.handle(noDiscount.build(bed.nextInboundId()));

        assertThat(listing().terms().discount()).isEqualTo(DiscountRule.NONE);
        assertThat(quotedTotal()).isEqualTo(Money.myr(220));
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A listing heard before the provider went live is kept, and the provider isn't bookable yet.")
    void listingOfAProviderNotYetLiveIsKept() {
        ListingTermsChangedIE event = new ReservationTestBed().kemasListing().build("listing-first");

        bed.listingTermsChanged.handle(new ListingTermsChangedIE("listing-new", "provider-new", event.cities(),
                event.jobCodes(), event.hourlyRatePerHelper(), event.travelFeeKind(), event.flatTravelFee(),
                event.travelFeeByCity(), event.discountRate(), event.discountMinHours(), event.discountMinHelpers(),
                event.minBookingHours(), event.maxBookingHours(), event.bookingStep(), event.minHelpers(),
                event.maxHelpers(), event.minimumNotice(), event.jobBuffer(), event.acceptancePolicy(),
                event.cancellationTier(), event.noticeRaised(), event.occurredAt()));

        ProviderReplica replica = bed.providers.findById("provider-new").orElseThrow();
        assertThat(replica.bookable()).isFalse();
        assertThat(replica.listing()).isEqualTo(Optional.of(listing()));
    }

    @Test
    @Tag("RES-8")
    @DisplayName("A redelivered listing changes nothing, even after a newer one.")
    void redeliveredListingIsIgnored() {
        ListingTermsChangedIE original = bed.kemasListing().build("listing-1");
        bed.listingTermsChanged.handle(original);
        ListingEvent raised = bed.kemasListing();
        raised.rate = Money.myr(30);
        bed.listingTermsChanged.handle(raised.build(bed.nextInboundId()));

        bed.listingTermsChanged.handle(original);

        assertThat(listing().terms().hourlyRatePerHelper()).isEqualTo(Money.myr(30));
        assertThat(HandleListingTermsChangedIE.HANDLER).isEqualTo("reservation.ListingTermsChanged");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
