package offering.usecase;

import static offering.support.OfferingFixtures.KITCHEN;
import static offering.support.OfferingFixtures.LAUNDRY;
import static offering.support.OfferingFixtures.MOP;
import static offering.support.OfferingFixtures.NOW;
import static offering.support.OfferingFixtures.PROVIDER;
import static offering.support.OfferingFixtures.VACUUM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ie.events.OfferingChangedIE;
import ie.events.ProviderListingStoppedIE;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kernel.City;
import kernel.Money;
import offering.domain.AcceptancePolicy;
import offering.domain.CancellationTier;
import offering.domain.HelperRange;
import offering.domain.HoursRange;
import offering.domain.InsuranceDeclaration;
import offering.domain.ProviderProfile;
import offering.domain.SupervisorRule;
import offering.domain.TravelFee;
import offering.domain.exception.NoJobOfferedException;
import offering.domain.exception.NotReadyToGoLiveException;
import offering.support.OfferingWorld;
import offering.usecase.dto.ChooseAcceptancePolicyRequest;
import offering.usecase.dto.ChooseCancellationTierRequest;
import offering.usecase.dto.DeclareInsuranceRequest;
import offering.usecase.dto.GoLiveRequest;
import offering.usecase.dto.SetCitiesRequest;
import offering.usecase.dto.SetHelperRangeRequest;
import offering.usecase.dto.SetHourlyRateRequest;
import offering.usecase.dto.SetHoursRequest;
import offering.usecase.dto.SetJobsRequest;
import offering.usecase.dto.SetProfileRequest;
import offering.usecase.dto.SetSupervisorRuleRequest;
import offering.usecase.dto.SetToolsRequest;
import offering.usecase.dto.SetTravelFeeRequest;
import offering.usecase.dto.ViewOfferingRequest;
import offering.usecase.dto.ViewOfferingResponse;
import offering.usecase.exception.HomaHoursNotKnownException;
import offering.usecase.exception.OfferingNotFoundException;
import offering.usecase.exception.UnknownChoiceException;
import org.junit.jupiter.api.Test;

class ProviderOfferingUsecasesTest {

    private final OfferingWorld world = new OfferingWorld();

    private OfferingChangedIE lastAnnounced() {
        List<OfferingChangedIE> announced = world.publisher.published(OfferingChangedIE.class);
        return announced.get(announced.size() - 1);
    }

    @Test
    void aSetProfileIsSavedAndAnnounced() {
        world.newProvider();

        String listing = new ProviderSetProfile(world.offerings, world.publisher, world.clock)
                .set(new SetProfileRequest(PROVIDER, "Bersih Home Care", "pictures/bersih.jpg")).listing();

        assertThat(listing).isEqualTo("DRAFT");
        assertThat(world.offering().state().profile()).isEqualTo(new ProviderProfile("Bersih Home Care", "pictures/bersih.jpg"));
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(2);
        assertThat(lastAnnounced().profileName()).isEqualTo("Bersih Home Care");
        assertThat(lastAnnounced().profilePicture()).isEqualTo("pictures/bersih.jpg");
        assertThat(lastAnnounced().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void jobsAndToolsArePickedFromHomasLists() {
        world.newProvider();

        assertThat(new ProviderSetJobs(world.offerings, world.catalogues, world.publisher, world.clock)
                .set(new SetJobsRequest(PROVIDER, Set.of(KITCHEN, LAUNDRY))).listing()).isEqualTo("DRAFT");
        new ProviderSetTools(world.offerings, world.catalogues, world.publisher, world.clock)
                .set(new SetToolsRequest(PROVIDER, Set.of(VACUUM, MOP)));

        assertThat(lastAnnounced().jobIds()).containsExactlyInAnyOrder(KITCHEN, LAUNDRY);
        assertThat(lastAnnounced().toolIds()).containsExactlyInAnyOrder(VACUUM, MOP);
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(3);
    }

    @Test
    void aRefusedChangeIsNeitherSavedNorAnnounced() {
        world.newProvider();
        ProviderSetJobs usecase = new ProviderSetJobs(world.offerings, world.catalogues, world.publisher, world.clock);

        assertThatThrownBy(() -> usecase.set(new SetJobsRequest(PROVIDER, Set.of()))).isInstanceOf(NoJobOfferedException.class);
        assertThatThrownBy(() -> usecase.set(new SetJobsRequest("nobody", Set.of(KITCHEN))))
                .isInstanceOf(OfferingNotFoundException.class);

        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(1);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    void theTermsAreSetOneByOne() {
        world.readyProvider();

        assertThat(new ProviderSetHelperRange(world.offerings, world.publisher, world.clock).set(new SetHelperRangeRequest(PROVIDER, 2, 2)).listing()).isEqualTo("DRAFT");
        new ProviderSetSupervisorRule(world.offerings, world.publisher, world.clock)
                .set(new SetSupervisorRuleRequest(PROVIDER, 2, 4));
        assertThat(new ProviderSetHourlyRate(world.offerings, world.publisher, world.clock)
                .set(new SetHourlyRateRequest(PROVIDER, Money.ofRinggit(35))).listing()).isEqualTo("DRAFT");
        new ProviderSetTravelFee(world.offerings, world.publisher, world.clock)
                .set(new SetTravelFeeRequest(PROVIDER, false, Money.ZERO));
        assertThat(new ProviderSetHours(world.offerings, world.homaHours, world.publisher, world.clock)
                .set(new SetHoursRequest(PROVIDER, 1, 6)).listing()).isEqualTo("DRAFT");
        new ProviderSetCities(world.offerings, world.publisher, world.clock)
                .set(new SetCitiesRequest(PROVIDER, Set.of(City.SALAK_TINGGI)));
        assertThat(new ProviderChooseCancellationTier(world.offerings, world.publisher, world.clock)
                .choose(new ChooseCancellationTierRequest(PROVIDER, "FIRM")).listing()).isEqualTo("DRAFT");
        new ProviderChooseAcceptancePolicy(world.offerings, world.publisher, world.clock)
                .choose(new ChooseAcceptancePolicyRequest(PROVIDER, "INSTANT_BOOK"));
        assertThat(new ProviderDeclareInsurance(world.offerings, world.publisher, world.clock)
                .declare(new DeclareInsuranceRequest(PROVIDER, "Etiqa", NOW.plusSeconds(86400))).listing()).isEqualTo("DRAFT");

        var state = world.offering().state();
        assertThat(state.helpers()).isEqualTo(new HelperRange(2, 2));
        assertThat(state.supervisorRule()).isEqualTo(new SupervisorRule(2, 4));
        assertThat(state.hourlyRate()).isEqualTo(Money.ofRinggit(35));
        assertThat(state.travelFee()).isEqualTo(TravelFee.included());
        assertThat(state.hours()).isEqualTo(new HoursRange(1, 6));
        assertThat(state.cities()).containsExactly(City.SALAK_TINGGI);
        assertThat(state.cancellationTier()).isEqualTo(CancellationTier.FIRM);
        assertThat(state.acceptancePolicy()).isEqualTo(AcceptancePolicy.INSTANT_BOOK);
        assertThat(state.insurance()).isEqualTo(new InsuranceDeclaration("Etiqa", NOW.plusSeconds(86400)));
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(10);
        assertThat(world.publisher.published(OfferingChangedIE.class)).hasSize(9);
        assertThat(lastAnnounced()).isEqualTo(new OfferingChangedIE(PROVIDER, "DRAFT", "Bersih Home Care",
                "pictures/bersih.jpg", Set.of(KITCHEN, LAUNDRY), Set.of(VACUUM), 2, 2, 2, 4, Money.ofRinggit(35), false,
                Money.ZERO, 1, 6, Set.of(City.SALAK_TINGGI), "FIRM", "INSTANT_BOOK", "Etiqa", NOW.plusSeconds(86400), NOW));
    }

    @Test
    void hoursCannotBeSetBeforeHomasRangeIsKnown() {
        world.newProvider();
        world.announcedHours = Optional.empty();

        assertThatThrownBy(() -> new ProviderSetHours(world.offerings, world.homaHours, world.publisher, world.clock)
                .set(new SetHoursRequest(PROVIDER, 1, 6))).isInstanceOf(HomaHoursNotKnownException.class);
    }

    @Test
    void onlyHomasOptionsCanBeChosen() {
        world.newProvider();

        assertThatThrownBy(() -> new ProviderChooseCancellationTier(world.offerings, world.publisher, world.clock)
                .choose(new ChooseCancellationTierRequest(PROVIDER, "STRICT"))).isInstanceOf(UnknownChoiceException.class);
        assertThatThrownBy(() -> new ProviderChooseAcceptancePolicy(world.offerings, world.publisher, world.clock)
                .choose(new ChooseAcceptancePolicyRequest(PROVIDER, null))).isInstanceOf(UnknownChoiceException.class);
    }

    @Test
    void aProviderGoesLiveAndIsAnnouncedAsLive() {
        world.readyProvider();

        String listing = new ProviderGoLive(world.offerings, world.publisher, world.clock)
                .goLive(new GoLiveRequest(PROVIDER)).listing();

        assertThat(listing).isEqualTo("LIVE");
        assertThat(world.offering().isLive()).isTrue();
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(2);
        assertThat(lastAnnounced().listing()).isEqualTo("LIVE");
        assertThat(world.publisher.published(ProviderListingStoppedIE.class)).isEmpty();
    }

    @Test
    void goingLiveTooEarlyFailsWithEverythingMissing() {
        world.newProvider();

        assertThatThrownBy(() -> new ProviderGoLive(world.offerings, world.publisher, world.clock)
                .goLive(new GoLiveRequest(PROVIDER))).isInstanceOf(NotReadyToGoLiveException.class);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    void aNewProviderSeesAnEmptyOfferingAndWhatIsMissing() {
        world.newProvider();

        ViewOfferingResponse view = new ProviderViewOffering(world.offerings, world.clock).view(new ViewOfferingRequest(PROVIDER));

        assertThat(view.listing()).isEqualTo("DRAFT");
        assertThat(view.stoppedBecause()).isNull();
        assertThat(view.missingToGoLive()).contains("BUSINESS_VERIFIED", "PROFILE", "PAYOUT_ACCOUNT").hasSize(10);
        assertThat(view.profileName()).isNull();
        assertThat(view.hourlyRate()).isNull();
        assertThat(view.travelFee()).isNull();
        assertThat(view.travelFeeSeparate()).isFalse();
        assertThat(view.minHelpers()).isZero();
        assertThat(view.maxHours()).isZero();
        assertThat(view.cancellationTier()).isNull();
        assertThat(view.acceptancePolicy()).isEqualTo("REQUEST_AND_ACCEPT");
        assertThat(view.insurer()).isNull();
        assertThat(view.insuredUntil()).isNull();
    }

    @Test
    void aLiveProviderSeesTheirWholeOffering() {
        world.liveProvider();

        ViewOfferingResponse view = new ProviderViewOffering(world.offerings, world.clock).view(new ViewOfferingRequest(PROVIDER));

        assertThat(view).isEqualTo(new ViewOfferingResponse("LIVE", null, List.of(), "Bersih Home Care",
                "pictures/bersih.jpg", Set.of(KITCHEN, LAUNDRY), Set.of(VACUUM), 1, 2, 0, 0, Money.ofRinggit(30), true,
                Money.ofRinggit(10), 2, 5, Set.of(City.CYBERJAYA, City.PUTRAJAYA), "STANDARD", "REQUEST_AND_ACCEPT",
                null, null, 2, 1));
    }
}
