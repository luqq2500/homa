package offering.domain;

import static offering.domain.OfferingBuilders.aCompleteListing;
import static offering.domain.OfferingBuilders.aLiveListing;
import static offering.domain.OfferingBuilders.aRosterOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import offering.domain.exception.ListingAlreadyPausedException;
import offering.domain.exception.ListingNotLiveException;
import offering.domain.exception.ListingNotPausedException;
import org.junit.jupiter.api.Test;

/** Feature: Provider re-verification. A lapsed provider's listing is paused, and resumes when they pass. */
class ProviderListingPauseTest {

    @Test
    void aLiveListingTakesRequests() {
        ProviderListing listing = aLiveListing();

        assertThat(listing.isPaused()).isFalse();
        assertThat(listing.takesRequests()).isTrue();
    }

    @Test
    void aPausedListingStaysLiveButTakesNoNewRequests() {
        ProviderListing listing = aLiveListing();

        listing.pause();

        assertThat(listing.isLive()).isTrue();
        assertThat(listing.isPaused()).isTrue();
        assertThat(listing.takesRequests()).isFalse();
        assertThat(listing.snapshot().takesRequests()).isFalse();
    }

    @Test
    void aPausedListingResumesWhenTheProviderPasses() {
        ProviderListing listing = aLiveListing();
        listing.pause();

        listing.resume();

        assertThat(listing.isPaused()).isFalse();
        assertThat(listing.takesRequests()).isTrue();
        assertThat(listing.snapshot().takesRequests()).isTrue();
    }

    @Test
    void onlyALiveListingIsPaused() {
        ProviderListing draft = aCompleteListing();

        assertThatThrownBy(draft::pause).isInstanceOf(ListingNotLiveException.class);
        assertThat(draft.isPaused()).isFalse();
        assertThat(draft.takesRequests()).isFalse();
    }

    @Test
    void aPausedListingIsNotPausedTwice() {
        ProviderListing listing = aLiveListing();
        listing.pause();

        assertThatThrownBy(listing::pause)
                .isInstanceOf(ListingAlreadyPausedException.class)
                .hasMessage("The listing is already paused");
    }

    @Test
    void onlyAPausedListingResumes() {
        ProviderListing listing = aLiveListing();

        assertThatThrownBy(listing::resume)
                .isInstanceOf(ListingNotPausedException.class)
                .hasMessage("The listing is not paused");
    }

    @Test
    void aPausedListingThatGoesOfflineIsNoLongerPaused() {
        ProviderListing listing = aLiveListing();
        listing.pause();

        listing.updateRoster(TeamRoster.empty());

        assertThat(listing.isLive()).isFalse();
        assertThat(listing.isPaused()).isFalse();
    }

    @Test
    void aTeamChangeDoesNotPauseALiveListing() {
        ProviderListing listing = aLiveListing();

        listing.updateRoster(aRosterOf(3));

        assertThat(listing.isPaused()).isFalse();
        assertThat(listing.takesRequests()).isTrue();
    }

    @Test
    void aPausedListingStaysPausedWhenItsTeamChanges() {
        ProviderListing listing = aLiveListing();
        listing.pause();

        listing.updateRoster(aRosterOf(3));

        assertThat(listing.isLive()).isTrue();
        assertThat(listing.isPaused()).isTrue();
    }
}
