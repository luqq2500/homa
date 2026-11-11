package offering.domain;

import static offering.domain.OfferingBuilders.CATALOGUE_JOBS;
import static offering.domain.OfferingBuilders.HOMA_LIMITS;
import static offering.domain.OfferingBuilders.LAUNCH_AREA;
import static offering.domain.OfferingBuilders.aCompleteListing;
import static offering.domain.OfferingBuilders.aDraftWithATeamOf;
import static offering.domain.OfferingBuilders.aLiveListing;
import static offering.domain.OfferingBuilders.aRosterOf;
import static offering.domain.OfferingBuilders.city;
import static offering.domain.OfferingBuilders.job;
import static offering.domain.OfferingBuilders.tool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;

import kernel.Money;
import offering.domain.exception.ListingAlreadyLiveException;
import offering.domain.exception.ListingNotLiveException;
import offering.domain.exception.ListingNotReadyException;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Feature: Going live. */
class ProviderListingGoLiveTest {

    @Nested
    class ACompleteListing {

        @Test
        void aCompleteVerifiedProviderGoesLive() {
            ProviderListing listing = aCompleteListing();

            listing.goLive(true);

            assertThat(listing.isLive()).isTrue();
        }

        @Test
        void aCompleteListingIsMissingNothing() {
            assertThat(aCompleteListing().missingForGoLive()).isEmpty();
        }

        @Test
        void aPendingInviteDoesNotBlockGoLive() {
            ProviderListing listing = aDraftWithATeamOf(2);
            listing.setProfile(OfferingBuilders.A_PROFILE);
            listing.offerJobs(Set.of(job("KITCHEN")), CATALOGUE_JOBS);
            listing.setPricing(HourlyRate.of(Money.parse("30")), TravelFee.included());
            listing.setHours(HoursRange.of(2, 4), HOMA_LIMITS);
            listing.setHelpers(HelpersRange.of(1, 2));
            listing.coverCities(Set.of(city("Cyberjaya")), LAUNCH_AREA);
            listing.chooseCancellationTier(CancellationTier.STANDARD);

            listing.goLive(true);

            assertThat(listing.isLive()).isTrue();
            assertThat(listing.roster().helperCount()).isEqualTo(2);
        }

        @Test
        void anUnverifiedProviderOrMissingPayoutAccountBlocksGoLive() {
            ProviderListing listing = aCompleteListing();

            assertThatThrownBy(() -> listing.goLive(false))
                    .isInstanceOf(ListingNotReadyException.class)
                    .extracting(e -> ((ListingNotReadyException) e).missing(), collection(Object.class))
                    .containsExactly(GoLiveRequirement.PROVIDER_VERIFIED_AND_PAYOUT_READY);
        }

        @Test
        void aLiveListingDoesNotGoLiveAgain() {
            assertThatThrownBy(() -> aLiveListing().goLive(true)).isInstanceOf(ListingAlreadyLiveException.class);
        }
    }

    @Nested
    class MissingRequirements {

        @Test
        void missingRequirementsBlockGoLiveAndAreListed() {
            ProviderListing listing = aDraftWithATeamOf(2);
            listing.setProfile(Profile.withoutPicture("Kemas Home Care"));
            listing.offerJobs(Set.of(job("KITCHEN")), CATALOGUE_JOBS);
            listing.setPricing(HourlyRate.of(Money.parse("30")), TravelFee.included());
            listing.setHours(HoursRange.of(2, 4), HOMA_LIMITS);
            listing.setHelpers(HelpersRange.of(1, 2));
            listing.chooseCancellationTier(CancellationTier.FLEXIBLE);

            assertThatThrownBy(() -> listing.goLive(true))
                    .isInstanceOf(ListingNotReadyException.class)
                    .hasMessageContaining("profile picture")
                    .hasMessageContaining("cities")
                    .extracting(e -> ((ListingNotReadyException) e).missing(), collection(Object.class))
                    .containsExactlyInAnyOrder(GoLiveRequirement.PROFILE_PICTURE, GoLiveRequirement.CITIES);
        }

        @Test
        void aDraftIsMissingEverything() {
            assertThat(ProviderListing.draft("provider-1").missingForGoLive()).containsExactlyInAnyOrder(
                    GoLiveRequirement.PROFILE,
                    GoLiveRequirement.PROFILE_PICTURE,
                    GoLiveRequirement.JOBS,
                    GoLiveRequirement.PRICING,
                    GoLiveRequirement.HOURS,
                    GoLiveRequirement.HELPERS,
                    GoLiveRequirement.CITIES,
                    GoLiveRequirement.CANCELLATION_TIER);
        }

        @Test
        void aDraftWithAnUnverifiedProviderListsEverythingMissingButASupervisorItNeverAskedFor() {
            assertThatThrownBy(() -> ProviderListing.draft("provider-1").goLive(false))
                    .isInstanceOf(ListingNotReadyException.class)
                    .extracting(e -> ((ListingNotReadyException) e).missing(), collection(Object.class))
                    .hasSize(9)
                    .containsAll(EnumSet.complementOf(EnumSet.of(GoLiveRequirement.SUPERVISOR)));
        }

        @Test
        void requirementsAreShownInWords() {
            assertThat(GoLiveRequirement.PROFILE_PICTURE.description()).isEqualTo("profile picture");
            assertThat(GoLiveRequirement.CITIES).hasToString("cities");
        }
    }

    @Nested
    class TheSnapshotAReservationKeeps {

        @Test
        void aLiveListingIsCopiedIntoASnapshot() {
            OfferingSnapshot snapshot = aLiveListing().snapshot();

            assertThat(snapshot.providerId()).isEqualTo("provider-1");
            assertThat(snapshot.hourlyRate()).isEqualTo(HourlyRate.of(Money.parse("30")));
            assertThat(snapshot.travelFee()).isEqualTo(TravelFee.separate(Money.parse("20")));
            assertThat(snapshot.hours()).isEqualTo(HoursRange.of(2, 4));
            assertThat(snapshot.helpers()).isEqualTo(HelpersRange.of(1, 4));
            assertThat(snapshot.jobs()).containsExactlyInAnyOrder(job("KITCHEN"), job("LAUNDRY_FOLD"));
            assertThat(snapshot.tools()).containsExactly(tool("STEAM_MOP"));
            assertThat(snapshot.cities()).containsExactly(city("Cyberjaya"));
            assertThat(snapshot.cancellationTier()).isEqualTo(CancellationTier.STANDARD);
            assertThat(snapshot.roster()).isEqualTo(aRosterOf(4));
            assertThat(snapshot.takesRequests()).isTrue();
        }

        @Test
        void aLaterChangeDoesNotTouchASnapshotAlreadyTaken() {
            ProviderListing listing = aLiveListing();
            OfferingSnapshot before = listing.snapshot();

            listing.coverCities(Set.of(city("Putrajaya")), LAUNCH_AREA);

            assertThat(before.cities()).containsExactly(city("Cyberjaya"));
            assertThat(listing.snapshot().cities()).containsExactly(city("Putrajaya"));
        }

        @Test
        void onlyALiveListingCanBeReserved() {
            assertThatThrownBy(() -> aCompleteListing().snapshot()).isInstanceOf(ListingNotLiveException.class);
        }
    }
}
