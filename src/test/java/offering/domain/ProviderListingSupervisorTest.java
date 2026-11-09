package offering.domain;

import static offering.domain.OfferingBuilders.aCompleteListing;
import static offering.domain.OfferingBuilders.aLiveListing;
import static offering.domain.OfferingBuilders.aRosterOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import offering.domain.exception.InvalidSupervisorRuleException;
import offering.domain.exception.ListingNotReadyException;
import offering.domain.exception.NoSupervisorOnTeamException;
import java.util.OptionalInt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Feature: Supervisors. The provider has 4 helpers and 1 supervisor, sending a supervisor from 3 helpers or from 4 hours. */
class ProviderListingSupervisorTest {

    private static final SupervisorRule FROM_3_HELPERS_OR_4_HOURS = SupervisorRule.fromHelpersOrHours(3, 4);

    private static ProviderListing aLiveListingWithASupervisor() {
        ProviderListing listing = aLiveListing();
        listing.updateRoster(aRosterOf(4, 1));
        listing.setSupervisorRule(FROM_3_HELPERS_OR_4_HOURS);
        return listing;
    }

    @Nested
    class TheRule {

        @ParameterizedTest(name = "{0} helpers for {1} hours: supervisor required is {2}")
        @CsvSource({"2, 3, false", "3, 2, true", "1, 4, true", "3, 4, true"})
        void aSupervisorIsRequiredFromEitherThreshold(int helpers, int hours, boolean required) {
            assertThat(FROM_3_HELPERS_OR_4_HOURS.requiresSupervisor(helpers, hours)).isEqualTo(required);
        }

        @ParameterizedTest(name = "from 3 helpers only: {0} helpers for {1} hours is {2}")
        @CsvSource({"2, 6, false", "3, 1, true"})
        void aHelpersOnlyRuleIgnoresHours(int helpers, int hours, boolean required) {
            assertThat(SupervisorRule.fromHelpers(3).requiresSupervisor(helpers, hours)).isEqualTo(required);
        }

        @ParameterizedTest(name = "from 4 hours only: {0} helpers for {1} hours is {2}")
        @CsvSource({"10, 3, false", "1, 4, true"})
        void anHoursOnlyRuleIgnoresHelpers(int helpers, int hours, boolean required) {
            assertThat(SupervisorRule.fromHours(4).requiresSupervisor(helpers, hours)).isEqualTo(required);
        }

        @Test
        void aRuleNeedsAThreshold() {
            assertThatThrownBy(() -> new SupervisorRule(OptionalInt.empty(), OptionalInt.empty()))
                    .isInstanceOf(InvalidSupervisorRuleException.class)
                    .hasMessageContaining("needs a minimum");
        }

        @Test
        void thresholdsAreAtLeastOne() {
            assertThatThrownBy(() -> SupervisorRule.fromHelpers(0))
                    .isInstanceOf(InvalidSupervisorRuleException.class)
                    .hasMessageContaining("helpers must be at least 1, was 0");
            assertThatThrownBy(() -> SupervisorRule.fromHours(0))
                    .isInstanceOf(InvalidSupervisorRuleException.class)
                    .hasMessageContaining("hours");
            assertThat(SupervisorRule.fromHelpersOrHours(1, 1).requiresSupervisor(1, 1)).isTrue();
            assertThatThrownBy(() -> new SupervisorRule(null, OptionalInt.empty())).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new SupervisorRule(OptionalInt.empty(), null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class OnTheListing {

        @Test
        void aSupervisorRuleNeedsASupervisorOnTheTeam() {
            ProviderListing listing = aCompleteListing();

            assertThatThrownBy(() -> listing.setSupervisorRule(SupervisorRule.fromHelpers(3)))
                    .isInstanceOf(NoSupervisorOnTeamException.class);
            assertThat(listing.supervisorRule()).isEmpty();
        }

        @Test
        void theProviderSetsAndClearsTheRule() {
            ProviderListing listing = aCompleteListing();
            listing.updateRoster(aRosterOf(4, 1));

            listing.setSupervisorRule(FROM_3_HELPERS_OR_4_HOURS);
            assertThat(listing.supervisorRule()).contains(FROM_3_HELPERS_OR_4_HOURS);

            listing.clearSupervisorRule();
            assertThat(listing.supervisorRule()).isEmpty();
        }

        @Test
        void goLiveNeedsASupervisorWhenARuleIsSet() {
            ProviderListing listing = aCompleteListing();
            listing.updateRoster(aRosterOf(4, 1));
            listing.setSupervisorRule(FROM_3_HELPERS_OR_4_HOURS);
            listing.updateRoster(aRosterOf(4));

            assertThat(listing.missingForGoLive()).containsExactly(GoLiveRequirement.SUPERVISOR);
            assertThatThrownBy(() -> listing.goLive(true))
                    .isInstanceOf(ListingNotReadyException.class)
                    .hasMessageContaining("supervisor");
        }

        @Test
        void aLiveListingWithARuleGoesOfflineWhenItsLastSupervisorLeaves() {
            ProviderListing listing = aLiveListingWithASupervisor();

            listing.updateRoster(aRosterOf(4));

            assertThat(listing.isLive()).isFalse();
            assertThat(listing.supervisorRule()).contains(FROM_3_HELPERS_OR_4_HOURS);
        }

        @Test
        void aLiveListingWithARuleStaysLiveWhileItHasASupervisor() {
            ProviderListing listing = aLiveListingWithASupervisor();

            listing.updateRoster(aRosterOf(3, 2));

            assertThat(listing.isLive()).isTrue();
        }

        @Test
        void aLiveListingWithNoRuleNeedsNoSupervisor() {
            ProviderListing listing = aLiveListing();

            listing.updateRoster(aRosterOf(3));

            assertThat(listing.isLive()).isTrue();
        }

        @Test
        void theSnapshotCarriesTheRuleAndTheProviderType() {
            OfferingSnapshot snapshot = aLiveListingWithASupervisor().snapshot();

            assertThat(snapshot.supervisorRule()).contains(FROM_3_HELPERS_OR_4_HOURS);
            assertThat(snapshot.providerType()).isEqualTo(ProviderType.INDEPENDENT);
            assertThat(snapshot.roster()).isEqualTo(aRosterOf(4, 1));
        }

        @Test
        void aListingKeepsItsProviderType() {
            assertThat(ProviderListing.draft("provider-1", ProviderType.BUSINESS).providerType()).isEqualTo(ProviderType.BUSINESS);
            assertThatThrownBy(() -> ProviderListing.draft("provider-1", null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> aCompleteListing().setSupervisorRule(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
