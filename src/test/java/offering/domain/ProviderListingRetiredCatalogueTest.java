package offering.domain;

import static offering.domain.OfferingBuilders.CATALOGUE_JOBS;
import static offering.domain.OfferingBuilders.aCompleteListing;
import static offering.domain.OfferingBuilders.aLiveListing;
import static offering.domain.OfferingBuilders.job;
import static offering.domain.OfferingBuilders.tool;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Feature: Retired jobs and tools. A job or tool Homa retires comes off every listing that has it. */
class ProviderListingRetiredCatalogueTest {

    @Test
    void aRetiredJobComesOffTheListing() {
        ProviderListing listing = aLiveListing();

        listing.withdrawJob(job("KITCHEN"));

        assertThat(listing.jobs()).containsExactly(job("LAUNDRY_FOLD"));
        assertThat(listing.isLive()).isTrue();
        assertThat(listing.isPaused()).isFalse();
        assertThat(listing.takesRequests()).isTrue();
        assertThat(listing.snapshot().jobs()).containsExactly(job("LAUNDRY_FOLD"));
    }

    @Test
    void aLiveListingLeftWithNoJobGoesOffline() {
        ProviderListing listing = aLiveListing();

        listing.withdrawJob(job("KITCHEN"));
        listing.withdrawJob(job("LAUNDRY_FOLD"));

        assertThat(listing.jobs()).isEmpty();
        assertThat(listing.isLive()).isFalse();
        assertThat(listing.takesRequests()).isFalse();
        assertThat(listing.missingForGoLive()).containsExactly(GoLiveRequirement.JOBS);
    }

    @Test
    void aPausedListingLeftWithNoJobIsNoLongerPaused() {
        ProviderListing listing = aLiveListing();
        listing.pause();

        listing.withdrawJob(job("KITCHEN"));
        listing.withdrawJob(job("LAUNDRY_FOLD"));

        assertThat(listing.isLive()).isFalse();
        assertThat(listing.isPaused()).isFalse();
    }

    @Test
    void aPausedListingThatStillHasAJobStaysPaused() {
        ProviderListing listing = aLiveListing();
        listing.pause();

        listing.withdrawJob(job("KITCHEN"));

        assertThat(listing.isLive()).isTrue();
        assertThat(listing.isPaused()).isTrue();
    }

    @Test
    void theListingGoesLiveAgainOnceTheProviderOffersAnotherJob() {
        ProviderListing listing = aLiveListing();
        listing.withdrawJob(job("KITCHEN"));
        listing.withdrawJob(job("LAUNDRY_FOLD"));

        listing.offerJobs(Set.of(job("TOILET_CARE")), CATALOGUE_JOBS);
        listing.goLive(true);

        assertThat(listing.isLive()).isTrue();
        assertThat(listing.snapshot().jobs()).containsExactly(job("TOILET_CARE"));
    }

    @Test
    void aRetiredJobTheListingDoesNotOfferChangesNothing() {
        ProviderListing listing = aLiveListing();

        listing.withdrawJob(job("TOILET_CARE"));

        assertThat(listing.jobs()).containsExactlyInAnyOrder(job("KITCHEN"), job("LAUNDRY_FOLD"));
        assertThat(listing.isLive()).isTrue();
    }

    @Test
    void aDraftLosesARetiredJobAndStaysADraft() {
        ProviderListing draft = aCompleteListing();

        draft.withdrawJob(job("KITCHEN"));

        assertThat(draft.jobs()).containsExactly(job("LAUNDRY_FOLD"));
        assertThat(draft.isLive()).isFalse();
    }

    @Test
    void aRequestAlreadyMadeKeepsTheRetiredJob() {
        ProviderListing listing = aLiveListing();
        OfferingSnapshot atRequest = listing.snapshot();

        listing.withdrawJob(job("KITCHEN"));

        assertThat(atRequest.jobs()).containsExactlyInAnyOrder(job("KITCHEN"), job("LAUNDRY_FOLD"));
    }

    @Test
    void aRetiredToolComesOffTheListingAndItStaysLive() {
        ProviderListing listing = aLiveListing();

        listing.withdrawTool(tool("STEAM_MOP"));

        assertThat(listing.tools()).isEmpty();
        assertThat(listing.isLive()).isTrue();
        assertThat(listing.snapshot().tools()).isEmpty();
    }

    @Test
    void aRetiredToolTheProviderDoesNotBringChangesNothing() {
        ProviderListing listing = aLiveListing();

        listing.withdrawTool(tool("VACUUM"));

        assertThat(listing.tools()).containsExactly(tool("STEAM_MOP"));
    }
}
