package offering.domain;

import static offering.support.OfferingFixtures.HOMA_HOURS;
import static offering.support.OfferingFixtures.KITCHEN;
import static offering.support.OfferingFixtures.LAUNDRY;
import static offering.support.OfferingFixtures.MOP;
import static offering.support.OfferingFixtures.PROVIDER;
import static offering.support.OfferingFixtures.VACUUM;
import static offering.support.OfferingFixtures.live;
import static offering.support.OfferingFixtures.readyToGoLive;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import offering.domain.exception.NotReadyToGoLiveException;
import org.junit.jupiter.api.Test;

class OfferingGoLiveTest {

    @Test
    void aProviderGoesLiveOnceEverythingIsSetUp() {
        Offering offering = readyToGoLive();
        assertThat(offering.missingToGoLive()).isEmpty();
        assertThat(offering.isLive()).isFalse();

        offering.goLive();

        assertThat(offering.isLive()).isTrue();
        assertThat(offering.listing()).isEqualTo(Listing.LIVE);
    }

    @Test
    void goingLiveTooEarlyListsEverythingStillMissing() {
        Offering offering = Offering.startFor(PROVIDER);

        assertThat(offering.missingToGoLive()).containsExactly(Missing.BUSINESS_VERIFIED, Missing.PROFILE, Missing.JOBS,
                Missing.CITIES, Missing.HOURLY_RATE, Missing.TRAVEL_FEE, Missing.HOURS, Missing.HELPERS,
                Missing.CANCELLATION_POLICY, Missing.PAYOUT_ACCOUNT);
        assertThatThrownBy(offering::goLive).isInstanceOf(NotReadyToGoLiveException.class)
                .hasMessageContaining("BUSINESS_VERIFIED").hasMessageContaining("PAYOUT_ACCOUNT");
        assertThat(offering.listing()).isEqualTo(Listing.DRAFT);
    }

    @Test
    void aSupervisorRuleWithNoSupervisorLeftBlocksGoingLive() {
        Offering offering = readyToGoLive();
        offering.setSupervisorRule(2, 0);
        offering.teamMemberLeft("supervisor-1");

        assertThat(offering.missingToGoLive()).containsExactly(Missing.SUPERVISOR);

        offering.setSupervisorRule(0, 0);
        assertThat(offering.missingToGoLive()).isEmpty();
    }

    @Test
    void theHelperRangeShrinksWithTheTeam() {
        Offering offering = live();
        offering.setHelperRange(2, 2);

        offering.teamMemberLeft("helper-2");

        assertThat(offering.state().helpers()).isEqualTo(new HelperRange(1, 1));
        assertThat(offering.isLive()).isTrue();
    }

    @Test
    void aGrowingTeamDoesNotWidenTheRange() {
        Offering offering = live();

        offering.teamMemberJoined("helper-3", TeamRole.HELPER);

        assertThat(offering.state().helpers()).isEqualTo(new HelperRange(1, 2));
        assertThat(offering.state().team().helpers()).isEqualTo(3);
    }

    @Test
    void movingAHelperToSupervisorLowersTheHelpersAndShrinksTheMaximum() {
        Offering offering = live();

        offering.teamMemberChangedRole("helper-2", TeamRole.SUPERVISOR);

        assertThat(offering.state().team().helpers()).isEqualTo(1);
        assertThat(offering.state().team().supervisors()).isEqualTo(2);
        assertThat(offering.state().helpers()).isEqualTo(new HelperRange(1, 1));
    }

    @Test
    void aLiveProviderWithNoHelperLeftStopsBeingLiveAndMustSetHelpersAgain() {
        Offering offering = live();

        offering.teamMemberLeft("helper-1");
        offering.teamMemberLeft("helper-2");

        assertThat(offering.listing()).isEqualTo(Listing.OFFLINE);
        assertThat(offering.state().stoppedBecause()).isEqualTo(StopReason.NO_HELPER);
        assertThat(offering.state().helpers()).isNull();
        assertThat(offering.missingToGoLive()).containsExactly(Missing.HELPERS);

        offering.teamMemberJoined("helper-3", TeamRole.HELPER);
        assertThat(offering.listing()).isEqualTo(Listing.OFFLINE);
        offering.setHelperRange(1, 1);
        offering.goLive();
        assertThat(offering.isLive()).isTrue();
        assertThat(offering.state().stoppedBecause()).isNull();
    }

    @Test
    void aLiveProviderWithASupervisorRuleAndNoSupervisorLeftStopsBeingLive() {
        Offering offering = live();
        offering.setSupervisorRule(0, 4);

        offering.teamMemberLeft("supervisor-1");

        assertThat(offering.listing()).isEqualTo(Listing.OFFLINE);
        assertThat(offering.state().stoppedBecause()).isEqualTo(StopReason.NO_SUPERVISOR);
    }

    @Test
    void withoutASupervisorRuleLosingTheSupervisorChangesNothing() {
        Offering offering = live();

        offering.teamMemberLeft("supervisor-1");

        assertThat(offering.isLive()).isTrue();
    }

    @Test
    void aDraftOfferingIsNeverTakenOffline() {
        Offering offering = readyToGoLive();

        offering.teamMemberLeft("helper-1");
        offering.teamMemberLeft("helper-2");

        assertThat(offering.listing()).isEqualTo(Listing.DRAFT);
        assertThat(offering.state().stoppedBecause()).isNull();
    }

    @Test
    void aRetiredJobComesOffTheListing() {
        Offering offering = live();

        assertThat(offering.retireJob(LAUNDRY)).isTrue();

        assertThat(offering.jobIds()).containsExactly(KITCHEN);
        assertThat(offering.isLive()).isTrue();
        assertThat(offering.retireJob(LAUNDRY)).isFalse();
    }

    @Test
    void aLiveProviderLeftWithNoJobIsTakenOfflineUntilTheyOfferAnother() {
        Offering offering = live();

        offering.retireJob(LAUNDRY);
        offering.retireJob(KITCHEN);

        assertThat(offering.jobIds()).isEmpty();
        assertThat(offering.listing()).isEqualTo(Listing.OFFLINE);
        assertThat(offering.state().stoppedBecause()).isEqualTo(StopReason.NO_JOB);
        assertThat(offering.missingToGoLive()).containsExactly(Missing.JOBS);
    }

    @Test
    void aRetiredToolComesOffTheListing() {
        Offering offering = live();

        assertThat(offering.retireTool(VACUUM)).isTrue();

        assertThat(offering.toolIds()).isEmpty();
        assertThat(offering.isLive()).isTrue();
        assertThat(offering.retireTool(MOP)).isFalse();
    }

    @Test
    void whenHomaNarrowsItsRangeTheProvidersHoursShrinkToFit() {
        Offering offering = live();

        assertThat(offering.fitHoursInto(new HoursRange(3, 4))).isTrue();

        assertThat(offering.state().hours()).isEqualTo(new HoursRange(3, 4));
        assertThat(offering.isLive()).isTrue();
    }

    @Test
    void aProviderLeftWithNoHoursInsideHomasRangeGoesOffline() {
        Offering offering = live();

        assertThat(offering.fitHoursInto(new HoursRange(6, 6))).isTrue();

        assertThat(offering.state().hours()).isNull();
        assertThat(offering.listing()).isEqualTo(Listing.OFFLINE);
        assertThat(offering.state().stoppedBecause()).isEqualTo(StopReason.NO_HOURS);
        assertThat(offering.missingToGoLive()).containsExactly(Missing.HOURS);
        assertThat(offering.fitHoursInto(HOMA_HOURS)).isFalse();
    }

    @Test
    void wideningHomasRangeOrARangeThatStillFitsChangesNoHours() {
        Offering offering = live();

        assertThat(offering.fitHoursInto(new HoursRange(1, 8))).isFalse();
        assertThat(offering.fitHoursInto(new HoursRange(2, 5))).isFalse();

        assertThat(offering.state().hours()).isEqualTo(new HoursRange(2, 5));
        assertThat(new HoursRange(2, 5).fittedInto(new HoursRange(5, 9))).contains(new HoursRange(5, 5));
        assertThat(new HoursRange(2, 5).fittedInto(new HoursRange(1, 2))).contains(new HoursRange(2, 2));
        assertThat(new HoursRange(2, 5).fittedInto(new HoursRange(6, 9))).isEmpty();
    }

    @Test
    void aLapsedBusinessHasItsListingPausedUntilItPasses() {
        Offering offering = live();

        offering.businessLapsed();

        assertThat(offering.listing()).isEqualTo(Listing.PAUSED);
        assertThat(offering.state().stoppedBecause()).isEqualTo(StopReason.BUSINESS_LAPSED);
        assertThat(offering.missingToGoLive()).containsExactly(Missing.BUSINESS_VERIFIED);

        offering.businessVerified();

        assertThat(offering.isLive()).isTrue();
        assertThat(offering.state().stoppedBecause()).isNull();
    }

    @Test
    void verificationDoesNotPutADraftOrOfflineOfferingLive() {
        Offering draft = readyToGoLive();
        draft.businessLapsed();
        assertThat(draft.listing()).isEqualTo(Listing.DRAFT);
        draft.businessVerified();
        assertThat(draft.listing()).isEqualTo(Listing.DRAFT);

        Offering offline = live();
        offline.retireJob(KITCHEN);
        offline.retireJob(LAUNDRY);
        offline.businessLapsed();
        offline.businessVerified();
        assertThat(offline.listing()).isEqualTo(Listing.OFFLINE);
        assertThat(offline.state().stoppedBecause()).isEqualTo(StopReason.NO_JOB);
    }

    @Test
    void theTeamCountsHelpersAndSupervisorsSeparately() {
        Team team = Team.EMPTY.with("a", TeamRole.HELPER).with("b", TeamRole.SUPERVISOR).with("c", TeamRole.HELPER);

        assertThat(team.helpers()).isEqualTo(2);
        assertThat(team.supervisors()).isEqualTo(1);
        assertThat(team.without("a").helpers()).isEqualTo(1);
        assertThat(team.with("a", TeamRole.SUPERVISOR).supervisors()).isEqualTo(2);
        assertThat(Team.EMPTY.helpers()).isZero();
    }

    @Test
    void aHelperRangeShrinksToTheTeamOrDisappears() {
        HelperRange range = new HelperRange(2, 4);

        assertThat(range.shrunkTo(5)).contains(range);
        assertThat(range.shrunkTo(4)).contains(range);
        assertThat(range.shrunkTo(3)).contains(new HelperRange(2, 3));
        assertThat(range.shrunkTo(1)).contains(new HelperRange(1, 1));
        assertThat(range.shrunkTo(0)).isEmpty();
    }
}
