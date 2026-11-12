package offering.domain;

import static offering.domain.OfferingBuilders.HOMA_LIMITS;
import static offering.domain.OfferingBuilders.aCompleteListing;
import static offering.domain.OfferingBuilders.aLiveListing;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Feature: Homa narrows its duration range. A listing's hours keep inside Homa's range. */
class ProviderListingHomaHoursTest {

    private static ProviderListing aLiveListingWithHours(int min, int max) {
        ProviderListing listing = aLiveListing();
        listing.setHours(HoursRange.of(min, max), HOMA_LIMITS);
        return listing;
    }

    @ParameterizedTest(name = "hours {0} to {1} under Homa's {2} to {3} are {4}, {5} to {6}")
    @CsvSource({
        "2, 6, 1, 4, NARROWED,     2, 4",
        "1, 4, 2, 6, NARROWED,     2, 4",
        "4, 6, 1, 4, NARROWED,     4, 4",
        "2, 4, 1, 4, NOT_AFFECTED, 2, 4",
        "2, 4, 1, 6, NOT_AFFECTED, 2, 4",
        "2, 4, 2, 4, NOT_AFFECTED, 2, 4"
    })
    void aListingsHoursShrinkToFit(int min, int max, int homaMin, int homaMax, HoursFit result, int afterMin, int afterMax) {
        ProviderListing listing = aLiveListingWithHours(min, max);

        HoursFit fit = listing.fitHours(new HomaLimits(homaMin, homaMax));

        assertThat(fit).isEqualTo(result);
        assertThat(listing.hours()).contains(HoursRange.of(afterMin, afterMax));
        assertThat(listing.isLive()).isTrue();
        assertThat(listing.snapshot().hours()).isEqualTo(HoursRange.of(afterMin, afterMax));
    }

    @Test
    void aLiveListingLeftWithNoHoursGoesOffline() {
        ProviderListing listing = aLiveListingWithHours(5, 6);

        HoursFit fit = listing.fitHours(new HomaLimits(1, 4));

        assertThat(fit).isEqualTo(HoursFit.CLEARED_AND_OFFLINE);
        assertThat(listing.hours()).isEmpty();
        assertThat(listing.isLive()).isFalse();
        assertThat(listing.takesRequests()).isFalse();
        assertThat(listing.missingForGoLive()).containsExactly(GoLiveRequirement.HOURS);
    }

    @Test
    void aPausedListingLeftWithNoHoursIsNoLongerPaused() {
        ProviderListing listing = aLiveListingWithHours(5, 6);
        listing.pause();

        listing.fitHours(new HomaLimits(1, 4));

        assertThat(listing.isLive()).isFalse();
        assertThat(listing.isPaused()).isFalse();
    }

    @Test
    void aPausedListingWhoseHoursNarrowStaysPaused() {
        ProviderListing listing = aLiveListingWithHours(2, 6);
        listing.pause();

        listing.fitHours(new HomaLimits(1, 4));

        assertThat(listing.isLive()).isTrue();
        assertThat(listing.isPaused()).isTrue();
    }

    @Test
    void aDraftLeftWithNoHoursNeedsThemAgain() {
        ProviderListing listing = aCompleteListing();
        listing.setHours(HoursRange.of(5, 6), HOMA_LIMITS);

        HoursFit fit = listing.fitHours(new HomaLimits(1, 4));

        assertThat(fit).isEqualTo(HoursFit.CLEARED);
        assertThat(listing.hours()).isEmpty();
        assertThat(listing.isLive()).isFalse();
        assertThat(listing.missingForGoLive()).containsExactly(GoLiveRequirement.HOURS);
    }

    @Test
    void aListingWithNoHoursYetIsNotAffected() {
        ProviderListing listing = ProviderListing.draft("provider-1");

        assertThat(listing.fitHours(new HomaLimits(1, 4))).isEqualTo(HoursFit.NOT_AFFECTED);
        assertThat(listing.hours()).isEmpty();
    }

    @Test
    void theListingGoesLiveAgainOnceTheProviderSetsHoursInsideTheRange() {
        ProviderListing listing = aLiveListingWithHours(5, 6);
        listing.fitHours(new HomaLimits(1, 4));

        listing.setHours(HoursRange.of(2, 4), new HomaLimits(1, 4));
        listing.goLive(true);

        assertThat(listing.isLive()).isTrue();
    }
}
