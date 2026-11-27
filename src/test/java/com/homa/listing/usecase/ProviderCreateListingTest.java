package com.homa.listing.usecase;

import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.CYBERJAYA;
import static com.homa.listing.usecase.ListingTestBed.HANA;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_JOBS;
import static com.homa.listing.usecase.ListingTestBed.NOW;
import static com.homa.listing.usecase.ListingTestBed.PUTRAJAYA;
import static com.homa.listing.usecase.ListingTestBed.SALAK_TINGGI;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeCreation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.kernel.Money;
import com.homa.listing.domain.AcceptancePolicy;
import com.homa.listing.domain.CancellationTier;
import com.homa.listing.domain.City;
import com.homa.listing.domain.JobType;
import com.homa.listing.domain.ListingTerms;
import com.homa.listing.domain.Tool;
import com.homa.listing.domain.exception.BelowMinimumRateException;
import com.homa.listing.domain.exception.NotInPresetsException;
import com.homa.listing.usecase.dto.ProviderCreateListingRequest;
import com.homa.listing.usecase.dto.ProviderCreateListingResponse;
import com.homa.listing.usecase.exception.ListingAlreadyExistsException;
import com.homa.listing.usecase.exception.NotAllowedException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderCreateListingTest {

    private final ListingTestBed bed = new ListingTestBed().withHomaPresets();

    @Test
    @Tag("OFR-1")
    @DisplayName("Azman sets up Kemas Home's listing before it goes live: its choices are kept, defaults fill the rest, "
            + "and the full terms are published")
    void aProviderCreatesItsListingAndItsTermsArePublished() {
        bed.withKemasHomeTeam();

        ProviderCreateListingResponse response = bed.createListing.create(kemasHomeCreation());

        assertThat(response.providerId()).isEqualTo(KEMAS_HOME);
        ListingTerms stored = bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow();
        assertThat(stored.jobs()).extracting(JobType::code).containsExactlyInAnyOrderElementsOf(KEMAS_HOME_JOBS);
        assertThat(stored.offerings().tools()).extracting(Tool::code).containsExactly("HEPA_VACUUM");
        assertThat(stored.cities()).extracting(City::code).containsExactlyInAnyOrder(SALAK_TINGGI, CYBERJAYA, PUTRAJAYA);
        assertThat(stored.rate().perHelperPerHour()).isEqualTo(HOURLY_RATE_PER_HELPER);
        assertThat(stored.policy()).isEqualTo(AcceptancePolicy.REQUEST);
        assertThat(stored.tier()).isEqualTo(CancellationTier.STANDARD);
        assertThat(bed.owners.ownerOf(KEMAS_HOME_ID)).contains(AZMAN);

        ListingTermsChangedIE event = bed.publisher.single(ListingTermsChangedIE.class);
        assertThat(event.eventId()).isEqualTo("listing-event-1");
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(event.cities()).containsExactly(CYBERJAYA, PUTRAJAYA, SALAK_TINGGI);
        assertThat(event.jobCodes()).containsExactly("BATHROOM_CLEANING", "KITCHEN_CLEANING", "LAUNDRY_FOLDING");
        assertThat(event.hourlyRatePerHelper()).isEqualTo(Money.myr(25));
        assertThat(event.travelFeeKind()).isEqualTo("NONE");
        assertThat(event.flatTravelFee()).isNull();
        assertThat(event.travelFeeByCity()).isEmpty();
        assertThat(event.discountRate()).isNull();
        assertThat(event.discountMinHours()).isNull();
        assertThat(event.discountMinHelpers()).isZero();
        assertThat(event.minBookingHours()).isEqualTo(Duration.ofHours(1));
        assertThat(event.maxBookingHours()).isEqualTo(Duration.ofHours(8));
        assertThat(event.bookingStep()).isEqualTo(Duration.ofMinutes(15));
        assertThat(event.minHelpers()).isEqualTo(1);
        assertThat(event.maxHelpers()).isEqualTo(3);
        assertThat(event.minimumNotice()).isEqualTo(Duration.ofHours(2));
        assertThat(event.jobBuffer()).isEqualTo(Duration.ofMinutes(30));
        assertThat(event.acceptancePolicy()).isEqualTo("REQUEST");
        assertThat(event.cancellationTier()).isEqualTo("STANDARD");
        assertThat(event.noticeRaised()).isFalse();
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Before any helper is active, the default maximum of helpers is 0: nobody can be booked yet")
    void withoutActiveHelpersTheDefaultMaximumIsZero() {
        bed.createListing.create(kemasHomeCreation());

        assertThat(bed.publisher.single(ListingTermsChangedIE.class).maxHelpers()).isZero();
    }

    @Test
    @DisplayName("Once Provider names Azman as Kemas Home's owner, he can still create its listing")
    void theOwnerProviderNamedCreatesTheListing() {
        bed.withKemasHomeLive();

        bed.createListing.create(kemasHomeCreation());

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID)).isPresent();
        assertThat(bed.owners.ownerOf(KEMAS_HOME_ID)).contains(AZMAN);
    }

    @Test
    @DisplayName("Hana can't create Kemas Home's listing once Provider has named Azman its owner")
    void anotherPersonCantCreateTheListing() {
        bed.withKemasHomeLive();
        ProviderCreateListingRequest byHana = new ProviderCreateListingRequest(HANA, KEMAS_HOME, KEMAS_HOME_JOBS,
                List.of(), List.of(SALAK_TINGGI), HOURLY_RATE_PER_HELPER, "REQUEST", "STANDARD");

        assertThatThrownBy(() -> bed.createListing.create(byHana)).isInstanceOf(NotAllowedException.class);

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID)).isEmpty();
        assertThat(bed.owners.ownerOf(KEMAS_HOME_ID)).contains(AZMAN);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A provider has one listing: creating a second is refused and the first is kept")
    void aSecondListingIsRefused() {
        bed.createListing.create(kemasHomeCreation());
        bed.publisher.clear();

        assertThatThrownBy(() -> bed.createListing.create(kemasHomeCreation(Money.myr(40), "INSTANT")))
                .isInstanceOf(ListingAlreadyExistsException.class)
                .hasMessageContaining(KEMAS_HOME);

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow().rate().perHelperPerHour())
                .isEqualTo(HOURLY_RATE_PER_HELPER);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("OFR-1")
    @DisplayName("A city that isn't one of Homa's presets can't be chosen; nothing is stored or published")
    void aChoiceOutsideThePresetsIsRefused() {
        ProviderCreateListingRequest withKlang = new ProviderCreateListingRequest(AZMAN, KEMAS_HOME, KEMAS_HOME_JOBS,
                List.of(), List.of(SALAK_TINGGI, "KLANG"), HOURLY_RATE_PER_HELPER, "REQUEST", "STANDARD");

        assertThatThrownBy(() -> bed.createListing.create(withKlang)).isInstanceOf(NotInPresetsException.class);

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID)).isEmpty();
        assertThat(bed.owners.ownerOf(KEMAS_HOME_ID)).isEmpty();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("OFR-1")
    @DisplayName("Before Homa has any presets, nothing can be chosen")
    void withoutPresetsNothingCanBeChosen() {
        ListingTestBed empty = new ListingTestBed();

        assertThatThrownBy(() -> empty.createListing.create(kemasHomeCreation()))
                .isInstanceOf(NotInPresetsException.class);
    }

    @Test
    @Tag("OFR-11")
    @DisplayName("A rate under Homa's minimum of RM20 is refused")
    void aRateUnderTheMinimumIsRefused() {
        assertThatThrownBy(() -> bed.createListing.create(kemasHomeCreation(Money.myr(19), "REQUEST")))
                .isInstanceOf(BelowMinimumRateException.class);

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID)).isEmpty();
    }

    @Test
    @Tag("OFR-9")
    @DisplayName("An Instant provider's listing is published with its policy")
    void theChosenPolicyIsPublished() {
        bed.createListing.create(kemasHomeCreation(Money.myr(25), "INSTANT"));

        assertThat(bed.publisher.single(ListingTermsChangedIE.class).acceptancePolicy()).isEqualTo("INSTANT");
        assertThat(bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow().policy()).isEqualTo(AcceptancePolicy.INSTANT);
    }
}
