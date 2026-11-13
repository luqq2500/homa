package offering.domain;

import static offering.support.OfferingFixtures.HOMA_HOURS;
import static offering.support.OfferingFixtures.KITCHEN;
import static offering.support.OfferingFixtures.LAUNDRY;
import static offering.support.OfferingFixtures.MOP;
import static offering.support.OfferingFixtures.NOW;
import static offering.support.OfferingFixtures.PROVIDER;
import static offering.support.OfferingFixtures.VACUUM;
import static offering.support.OfferingFixtures.catalogue;
import static offering.support.OfferingFixtures.readyToGoLive;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import kernel.City;
import kernel.Money;
import offering.domain.exception.InsuranceIncompleteException;
import offering.domain.exception.InvalidHelperRangeException;
import offering.domain.exception.InvalidHourlyRateException;
import offering.domain.exception.InvalidHoursException;
import offering.domain.exception.InvalidSupervisorRuleException;
import offering.domain.exception.InvalidTravelFeeException;
import offering.domain.exception.JobNotOfferableException;
import offering.domain.exception.NoCityCoveredException;
import offering.domain.exception.NoJobOfferedException;
import offering.domain.exception.ProfileIncompleteException;
import offering.domain.exception.SupervisorRuleNeedsSupervisorException;
import offering.domain.exception.ToolNotPickableException;
import org.junit.jupiter.api.Test;

class OfferingSetUpTest {

    @Test
    void aNewOfferingHasNothingSetAndUsesRequestAndAccept() {
        OfferingState state = Offering.startFor(PROVIDER).state();

        assertThat(state.providerId()).isEqualTo(PROVIDER);
        assertThat(state.listing()).isEqualTo(Listing.DRAFT);
        assertThat(state.acceptancePolicy()).isEqualTo(AcceptancePolicy.REQUEST_AND_ACCEPT);
        assertThat(state.supervisorRule()).isEqualTo(SupervisorRule.NEVER);
        assertThat(state.jobIds()).isEmpty();
        assertThat(state.toolIds()).isEmpty();
        assertThat(state.cities()).isEmpty();
        assertThat(state.profile()).isNull();
        assertThat(state.helpers()).isNull();
        assertThat(state.businessVerified()).isFalse();
        assertThat(state.payoutAccountAdded()).isFalse();
        assertThat(state.team()).isEqualTo(Team.EMPTY);
    }

    @Test
    void aProfileHasANameAndAPicture() {
        Offering offering = Offering.startFor(PROVIDER);

        offering.setProfile(" Bersih Home Care ", "pictures/bersih.jpg");

        assertThat(offering.state().profile()).isEqualTo(new ProviderProfile("Bersih Home Care", "pictures/bersih.jpg"));
        assertThatThrownBy(() -> offering.setProfile(" ", "p.jpg")).isInstanceOf(ProfileIncompleteException.class);
        assertThatThrownBy(() -> offering.setProfile(null, "p.jpg")).isInstanceOf(ProfileIncompleteException.class);
        assertThatThrownBy(() -> offering.setProfile("Bersih", " ")).isInstanceOf(ProfileIncompleteException.class);
        assertThatThrownBy(() -> offering.setProfile("Bersih", null)).isInstanceOf(ProfileIncompleteException.class);
    }

    @Test
    void aProviderOffersAtLeastOneJobFromHomasJobList() {
        Offering offering = Offering.startFor(PROVIDER);

        offering.offerJobs(Set.of(KITCHEN), catalogue());

        assertThat(offering.jobIds()).containsExactly(KITCHEN);
        assertThatThrownBy(() -> offering.offerJobs(Set.of(), catalogue())).isInstanceOf(NoJobOfferedException.class);
        assertThatThrownBy(() -> offering.offerJobs(Set.of(KITCHEN, "job-plumbing"), catalogue()))
                .isInstanceOf(JobNotOfferableException.class);
        assertThat(offering.jobIds()).containsExactly(KITCHEN);
    }

    @Test
    void aProviderBringsToolsFromHomasToolListOrNone() {
        Offering offering = Offering.startFor(PROVIDER);

        offering.bringTools(Set.of(VACUUM, MOP), catalogue());
        assertThat(offering.toolIds()).containsExactlyInAnyOrder(VACUUM, MOP);

        assertThatThrownBy(() -> offering.bringTools(Set.of("tool-ladder"), catalogue()))
                .isInstanceOf(ToolNotPickableException.class);
        assertThat(offering.toolIds()).hasSize(2);

        offering.bringTools(Set.of(), catalogue());
        assertThat(offering.toolIds()).isEmpty();
    }

    @Test
    void aJobOrToolRetiredFromHomasListsCanNoLongerBePicked() {
        OfferableCatalogue catalogue = OfferableCatalogue.empty();
        catalogue.addJob(KITCHEN);
        catalogue.addJob(LAUNDRY);
        catalogue.addTool(VACUUM);
        catalogue.addTool(MOP);
        catalogue.retireJob(LAUNDRY);
        catalogue.retireTool(MOP);
        Offering offering = Offering.startFor(PROVIDER);

        assertThat(catalogue.jobIds()).containsExactly(KITCHEN);
        assertThat(catalogue.toolIds()).containsExactly(VACUUM);
        offering.offerJobs(Set.of(KITCHEN), catalogue);
        offering.bringTools(Set.of(VACUUM), catalogue);
        assertThatThrownBy(() -> offering.offerJobs(Set.of(LAUNDRY), catalogue)).isInstanceOf(JobNotOfferableException.class);
        assertThatThrownBy(() -> offering.bringTools(Set.of(MOP), catalogue)).isInstanceOf(ToolNotPickableException.class);
    }

    @Test
    void theHelperRangeStartsAtOneAndItsMaximumIsNoMoreThanTheHelpersOnTheTeam() {
        Offering offering = Offering.startFor(PROVIDER);
        offering.teamMemberJoined("helper-1", TeamRole.HELPER);
        offering.teamMemberJoined("helper-2", TeamRole.HELPER);
        offering.teamMemberJoined("supervisor-1", TeamRole.SUPERVISOR);

        offering.setHelperRange(1, 2);
        assertThat(offering.state().helpers()).isEqualTo(new HelperRange(1, 2));
        offering.setHelperRange(2, 2);
        assertThat(offering.state().helpers()).isEqualTo(new HelperRange(2, 2));

        assertThatThrownBy(() -> offering.setHelperRange(1, 3)).isInstanceOf(InvalidHelperRangeException.class);
        assertThatThrownBy(() -> offering.setHelperRange(0, 2)).isInstanceOf(InvalidHelperRangeException.class);
        assertThatThrownBy(() -> offering.setHelperRange(2, 1)).isInstanceOf(InvalidHelperRangeException.class);
        assertThat(offering.state().helpers()).isEqualTo(new HelperRange(2, 2));
    }

    @Test
    void aSupervisorRuleNeedsASupervisorOnTheTeamButCanAlwaysBeCleared() {
        Offering offering = Offering.startFor(PROVIDER);
        offering.teamMemberJoined("helper-1", TeamRole.HELPER);

        assertThatThrownBy(() -> offering.setSupervisorRule(3, 0)).isInstanceOf(SupervisorRuleNeedsSupervisorException.class);
        assertThatThrownBy(() -> offering.setSupervisorRule(0, 4)).isInstanceOf(SupervisorRuleNeedsSupervisorException.class);
        offering.setSupervisorRule(0, 0);
        assertThat(offering.state().supervisorRule().isSet()).isFalse();

        offering.teamMemberJoined("supervisor-1", TeamRole.SUPERVISOR);
        offering.setSupervisorRule(3, 4);
        assertThat(offering.state().supervisorRule()).isEqualTo(new SupervisorRule(3, 4));
        offering.setSupervisorRule(0, 4);
        assertThat(offering.state().supervisorRule().isSet()).isTrue();
        offering.setSupervisorRule(1, 0);
        assertThat(offering.state().supervisorRule().isSet()).isTrue();
        assertThatThrownBy(() -> offering.setSupervisorRule(-1, 0)).isInstanceOf(InvalidSupervisorRuleException.class);
        assertThatThrownBy(() -> offering.setSupervisorRule(0, -1)).isInstanceOf(InvalidSupervisorRuleException.class);
    }

    @Test
    void theHourlyRateIsMoreThanNothing() {
        Offering offering = Offering.startFor(PROVIDER);

        offering.setHourlyRate(Money.ofSen(1));

        assertThat(offering.state().hourlyRate()).isEqualTo(Money.ofSen(1));
        assertThatThrownBy(() -> offering.setHourlyRate(Money.ZERO)).isInstanceOf(InvalidHourlyRateException.class);
        assertThatThrownBy(() -> offering.setHourlyRate(null)).isInstanceOf(InvalidHourlyRateException.class);
    }

    @Test
    void aSeparateTravelFeeIsMoreThanNothingAndAnIncludedOneIsNothing() {
        Offering offering = Offering.startFor(PROVIDER);

        offering.setTravelFee(TravelFee.separate(Money.ofSen(1)));
        assertThat(offering.state().travelFee()).isEqualTo(new TravelFee(true, Money.ofSen(1)));
        offering.setTravelFee(TravelFee.included());
        assertThat(offering.state().travelFee()).isEqualTo(new TravelFee(false, Money.ZERO));

        assertThatThrownBy(() -> TravelFee.separate(Money.ZERO)).isInstanceOf(InvalidTravelFeeException.class);
        assertThatThrownBy(() -> new TravelFee(false, Money.ofRinggit(5))).isInstanceOf(InvalidTravelFeeException.class);
        assertThatThrownBy(() -> new TravelFee(true, null)).isInstanceOf(InvalidTravelFeeException.class);
    }

    @Test
    void aProvidersHoursAreInsideHomasRange() {
        Offering offering = Offering.startFor(PROVIDER);

        offering.setHours(1, 6, HOMA_HOURS);
        assertThat(offering.state().hours()).isEqualTo(new HoursRange(1, 6));
        offering.setHours(3, 3, HOMA_HOURS);
        assertThat(offering.state().hours()).isEqualTo(new HoursRange(3, 3));

        assertThatThrownBy(() -> offering.setHours(1, 7, HOMA_HOURS)).isInstanceOf(InvalidHoursException.class);
        assertThatThrownBy(() -> offering.setHours(1, 3, new HoursRange(2, 6))).isInstanceOf(InvalidHoursException.class);
        assertThatThrownBy(() -> offering.setHours(0, 3, HOMA_HOURS)).isInstanceOf(InvalidHoursException.class);
        assertThatThrownBy(() -> offering.setHours(4, 3, HOMA_HOURS)).isInstanceOf(InvalidHoursException.class);
        assertThat(offering.state().hours()).isEqualTo(new HoursRange(3, 3));
    }

    @Test
    void aProviderCoversAtLeastOneCity() {
        Offering offering = Offering.startFor(PROVIDER);

        offering.coverCities(Set.of(City.SALAK_TINGGI));

        assertThat(offering.state().cities()).containsExactly(City.SALAK_TINGGI);
        assertThatThrownBy(() -> offering.coverCities(Set.of())).isInstanceOf(NoCityCoveredException.class);
        assertThat(offering.state().cities()).containsExactly(City.SALAK_TINGGI);
    }

    @Test
    void aProviderChoosesTheirCancellationAndAcceptancePolicies() {
        Offering offering = Offering.startFor(PROVIDER);

        offering.chooseCancellationTier(CancellationTier.FIRM);
        offering.chooseAcceptancePolicy(AcceptancePolicy.INSTANT_BOOK);

        assertThat(offering.state().cancellationTier()).isEqualTo(CancellationTier.FIRM);
        assertThat(offering.state().acceptancePolicy()).isEqualTo(AcceptancePolicy.INSTANT_BOOK);
    }

    @Test
    void theInsuredBadgeShowsUntilTheDeclaredDate() {
        Offering offering = Offering.startFor(PROVIDER);
        assertThat(offering.isInsuredAt(NOW)).isFalse();

        offering.declareInsurance(" Etiqa ", NOW);

        assertThat(offering.state().insurance()).isEqualTo(new InsuranceDeclaration("Etiqa", NOW));
        assertThat(offering.isInsuredAt(NOW.minusSeconds(1))).isTrue();
        assertThat(offering.isInsuredAt(NOW)).isTrue();
        assertThat(offering.isInsuredAt(NOW.plusSeconds(1))).isFalse();
        assertThatThrownBy(() -> offering.declareInsurance(" ", NOW)).isInstanceOf(InsuranceIncompleteException.class);
        assertThatThrownBy(() -> offering.declareInsurance(null, NOW)).isInstanceOf(InsuranceIncompleteException.class);
        assertThatThrownBy(() -> offering.declareInsurance("Etiqa", null)).isInstanceOf(InsuranceIncompleteException.class);
    }

    @Test
    void aStoredOfferingIsRestoredExactly() {
        Offering offering = readyToGoLive();
        offering.setSupervisorRule(2, 4);
        offering.declareInsurance("Etiqa", NOW);
        offering.goLive();
        offering.businessLapsed();

        OfferingState state = offering.state();

        assertThat(Offering.restore(state).state()).isEqualTo(state);
        assertThat(state.listing()).isEqualTo(Listing.PAUSED);
        assertThat(state.stoppedBecause()).isEqualTo(StopReason.BUSINESS_LAPSED);
        assertThat(Offering.restore(state).providerId()).isEqualTo(PROVIDER);
    }
}
