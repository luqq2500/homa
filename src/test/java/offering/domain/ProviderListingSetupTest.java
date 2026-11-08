package offering.domain;

import static offering.domain.OfferingBuilders.CATALOGUE_JOBS;
import static offering.domain.OfferingBuilders.CATALOGUE_TOOLS;
import static offering.domain.OfferingBuilders.HOMA_LIMITS;
import static offering.domain.OfferingBuilders.LAUNCH_AREA;
import static offering.domain.OfferingBuilders.aDraftWithATeamOf;
import static offering.domain.OfferingBuilders.aLiveListing;
import static offering.domain.OfferingBuilders.aRosterOf;
import static offering.domain.OfferingBuilders.city;
import static offering.domain.OfferingBuilders.job;
import static offering.domain.OfferingBuilders.tool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kernel.Money;
import offering.domain.exception.CityNotInServiceAreaException;
import offering.domain.exception.HelpersExceedTeamSizeException;
import offering.domain.exception.HelpersOutOfRangeException;
import offering.domain.exception.HoursOutOfRangeException;
import offering.domain.exception.InvalidHoursRangeException;
import offering.domain.exception.JobNotInCatalogueException;
import offering.domain.exception.NoCitiesCoveredException;
import offering.domain.exception.NoJobsOfferedException;
import offering.domain.exception.OfferingDetailRequiredException;
import offering.domain.exception.ToolNotInCatalogueException;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Feature: Offering setup. */
class ProviderListingSetupTest {

    @Nested
    class ADraft {

        @Test
        void aNewListingIsADraftWithNothingSetUp() {
            ProviderListing listing = ProviderListing.draft("provider-1");

            assertThat(listing.providerId()).isEqualTo("provider-1");
            assertThat(listing.id()).isNotBlank();
            assertThat(listing.isLive()).isFalse();
            assertThat(listing.profile()).isEmpty();
            assertThat(listing.jobs()).isEmpty();
            assertThat(listing.tools()).isEmpty();
            assertThat(listing.hourlyRate()).isEmpty();
            assertThat(listing.travelFee()).isEmpty();
            assertThat(listing.hours()).isEmpty();
            assertThat(listing.helpers()).isEmpty();
            assertThat(listing.cities()).isEmpty();
            assertThat(listing.cancellationTier()).isEmpty();
            assertThat(listing.roster().size()).isZero();
        }

        @Test
        void aListingBelongsToAProvider() {
            assertThatThrownBy(() -> ProviderListing.draft(" ")).isInstanceOf(OfferingDetailRequiredException.class);
        }
    }

    @Nested
    class JobsAndTools {

        @Test
        void aProviderCanOnlyOfferJobsFromHomasJobList() {
            ProviderListing listing = ProviderListing.draft("provider-1");

            assertThatThrownBy(() -> listing.offerJobs(Set.of(job("KITCHEN"), job("DECLUTTER_GARAGE")), CATALOGUE_JOBS))
                    .isInstanceOf(JobNotInCatalogueException.class)
                    .hasMessageContaining("DECLUTTER_GARAGE");
            assertThat(listing.jobs()).isEmpty();
        }

        @Test
        void aProviderOffersJobsFromTheList() {
            ProviderListing listing = ProviderListing.draft("provider-1");

            listing.offerJobs(Set.of(job("KITCHEN"), job("LAUNDRY_FOLD")), CATALOGUE_JOBS);

            assertThat(listing.jobs()).containsExactlyInAnyOrder(job("KITCHEN"), job("LAUNDRY_FOLD"));
        }

        @Test
        void aProviderOffersAtLeastOneJob() {
            assertThatThrownBy(() -> ProviderListing.draft("provider-1").offerJobs(Set.of(), CATALOGUE_JOBS))
                    .isInstanceOf(NoJobsOfferedException.class);
        }

        @Test
        void aProviderCanOnlyBringToolsFromHomasToolList() {
            ProviderListing listing = ProviderListing.draft("provider-1");

            assertThatThrownBy(() -> listing.bringTools(Set.of(tool("LADDER")), CATALOGUE_TOOLS))
                    .isInstanceOf(ToolNotInCatalogueException.class)
                    .hasMessageContaining("LADDER");

            listing.bringTools(Set.of(tool("STEAM_MOP")), CATALOGUE_TOOLS);
            assertThat(listing.tools()).containsExactly(tool("STEAM_MOP"));

            listing.bringTools(Set.of(), CATALOGUE_TOOLS);
            assertThat(listing.tools()).isEmpty();
        }
    }

    @Nested
    class Hours {

        @ParameterizedTest(name = "hours {0} to {1} are accepted")
        @CsvSource({"1, 4", "2, 6", "3, 3"})
        void hoursWithinHomasRangeAreAccepted(int min, int max) {
            ProviderListing listing = ProviderListing.draft("provider-1");

            listing.setHours(HoursRange.of(min, max), HOMA_LIMITS);

            assertThat(listing.hours()).contains(HoursRange.of(min, max));
        }

        @ParameterizedTest(name = "hours {0} to {1} are outside Homa's range")
        @CsvSource({"0, 4", "2, 7"})
        void hoursMustSitWithinHomasRange(int min, int max) {
            assertThatThrownBy(() -> ProviderListing.draft("provider-1").setHours(HoursRange.of(min, max), HOMA_LIMITS))
                    .isInstanceOf(HoursOutOfRangeException.class);
        }

        @Test
        void theMinimumHoursCannotBeAboveTheMaximum() {
            assertThatThrownBy(() -> HoursRange.of(4, 2)).isInstanceOf(InvalidHoursRangeException.class);
        }
    }

    @Nested
    class Helpers {

        @Test
        void maxHelpersCannotExceedTheVerifiedTeamSize() {
            ProviderListing listing = aDraftWithATeamOf(3);

            assertThatThrownBy(() -> listing.setHelpers(HelpersRange.of(1, 4), HOMA_LIMITS))
                    .isInstanceOf(HelpersExceedTeamSizeException.class)
                    .hasMessageContaining("3");
            assertThat(listing.helpers()).isEmpty();
        }

        @Test
        void maxHelpersCanEqualTheVerifiedTeamSize() {
            ProviderListing listing = aDraftWithATeamOf(3);

            listing.setHelpers(HelpersRange.of(1, 3), HOMA_LIMITS);

            assertThat(listing.helpers()).contains(HelpersRange.of(1, 3));
        }

        @Test
        void maxHelpersCannotExceedTen() {
            assertThatThrownBy(() -> aDraftWithATeamOf(12).setHelpers(HelpersRange.of(1, 11), HOMA_LIMITS))
                    .isInstanceOf(HelpersOutOfRangeException.class);
            ProviderListing listing = aDraftWithATeamOf(12);
            listing.setHelpers(HelpersRange.of(1, 10), HOMA_LIMITS);
            assertThat(listing.helpers()).contains(HelpersRange.of(1, 10));
        }

        @Test
        void minHelpersIsAtLeastOne() {
            assertThatThrownBy(() -> aDraftWithATeamOf(3).setHelpers(HelpersRange.of(0, 2), HOMA_LIMITS))
                    .isInstanceOf(HelpersOutOfRangeException.class);
        }

        @Test
        void theHelperRangeShrinksWhenTheTeamShrinks() {
            ProviderListing listing = aLiveListing();

            listing.updateRoster(aRosterOf(3));

            assertThat(listing.helpers()).contains(HelpersRange.of(1, 3));
            assertThat(listing.isLive()).isTrue();
        }

        @Test
        void theMinimumShrinksTooWhenTheTeamFallsBelowIt() {
            ProviderListing listing = aDraftWithATeamOf(4);
            listing.setHelpers(HelpersRange.of(3, 4), HOMA_LIMITS);

            listing.updateRoster(aRosterOf(2));

            assertThat(listing.helpers()).contains(HelpersRange.of(2, 2));
        }

        @Test
        void aGrowingTeamDoesNotWidenTheRange() {
            ProviderListing listing = aDraftWithATeamOf(2);
            listing.setHelpers(HelpersRange.of(1, 2), HOMA_LIMITS);

            listing.updateRoster(aRosterOf(5));

            assertThat(listing.helpers()).contains(HelpersRange.of(1, 2));
            assertThat(listing.roster().size()).isEqualTo(5);
        }

        @Test
        void aDraftStaysADraftWhenItsTeamChanges() {
            ProviderListing listing = aDraftWithATeamOf(2);
            listing.setHelpers(HelpersRange.of(1, 2), HOMA_LIMITS);

            listing.updateRoster(aRosterOf(3));

            assertThat(listing.isLive()).isFalse();
        }

        @Test
        void aTeamOfOneLeavesARangeOfOneHelper() {
            ProviderListing listing = aLiveListing();

            listing.updateRoster(aRosterOf(1));

            assertThat(listing.helpers()).contains(HelpersRange.of(1, 1));
            assertThat(listing.isLive()).isTrue();
        }

        @Test
        void aLiveListingWithNoVerifiedTeamLeftIsNoLongerLive() {
            ProviderListing listing = aLiveListing();

            listing.updateRoster(TeamRoster.empty());

            assertThat(listing.helpers()).isEmpty();
            assertThat(listing.isLive()).isFalse();
            assertThat(listing.missingForGoLive()).containsExactly(GoLiveRequirement.HELPERS);
        }

        @Test
        void aTeamCanBeRecordedBeforeAnyHelperRangeIsSet() {
            ProviderListing listing = ProviderListing.draft("provider-1");

            listing.updateRoster(aRosterOf(2));

            assertThat(listing.helpers()).isEmpty();
            assertThat(listing.isLive()).isFalse();
        }
    }

    @Nested
    class Cities {

        @Test
        void citiesMustBeLaunchCities() {
            ProviderListing listing = ProviderListing.draft("provider-1");

            assertThatThrownBy(() -> listing.coverCities(Set.of(city("Cyberjaya"), city("Kajang")), LAUNCH_AREA))
                    .isInstanceOf(CityNotInServiceAreaException.class)
                    .hasMessageContaining("Kajang");
            assertThat(listing.cities()).isEmpty();
        }

        @Test
        void aProviderCoversLaunchCities() {
            ProviderListing listing = ProviderListing.draft("provider-1");

            listing.coverCities(Set.of(city("Cyberjaya"), city("Putrajaya")), LAUNCH_AREA);

            assertThat(listing.cities()).containsExactlyInAnyOrder(city("Cyberjaya"), city("Putrajaya"));
        }

        @Test
        void aProviderCoversAtLeastOneCity() {
            assertThatThrownBy(() -> ProviderListing.draft("provider-1").coverCities(Set.of(), LAUNCH_AREA))
                    .isInstanceOf(NoCitiesCoveredException.class);
        }
    }

    @Nested
    class PricingProfileAndTier {

        @Test
        void aProviderSetsTheirRateTravelFeeProfileAndTier() {
            ProviderListing listing = ProviderListing.draft("provider-1");

            listing.setPricing(HourlyRate.of(Money.parse("35")), TravelFee.included());
            listing.setProfile(Profile.of("Kemas Home Care", "photos/kemas.jpg"));
            listing.chooseCancellationTier(CancellationTier.FIRM);

            assertThat(listing.hourlyRate()).contains(HourlyRate.of(Money.parse("35")));
            assertThat(listing.travelFee()).contains(TravelFee.included());
            assertThat(listing.profile()).contains(Profile.of("Kemas Home Care", "photos/kemas.jpg"));
            assertThat(listing.cancellationTier()).contains(CancellationTier.FIRM);
        }

        @Test
        void pricingProfileAndTierCannotBeLeftOut() {
            ProviderListing listing = ProviderListing.draft("provider-1");

            assertThatThrownBy(() -> listing.setPricing(null, TravelFee.included())).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> listing.setPricing(HourlyRate.of(Money.parse("35")), null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> listing.setProfile(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> listing.chooseCancellationTier(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> listing.updateRoster(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
