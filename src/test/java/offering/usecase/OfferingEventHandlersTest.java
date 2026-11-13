package offering.usecase;

import static offering.support.OfferingFixtures.KITCHEN;
import static offering.support.OfferingFixtures.LAUNDRY;
import static offering.support.OfferingFixtures.MOP;
import static offering.support.OfferingFixtures.NOW;
import static offering.support.OfferingFixtures.PROVIDER;
import static offering.support.OfferingFixtures.VACUUM;
import static org.assertj.core.api.Assertions.assertThat;

import ie.events.BusinessFlagClearedIE;
import ie.events.BusinessLapsedIE;
import ie.events.BusinessRegisteredIE;
import ie.events.BusinessSeriouslyFlaggedIE;
import ie.events.BusinessVerifiedIE;
import ie.events.HomaValuesChangedIE;
import ie.events.JobAddedIE;
import ie.events.JobRetiredIE;
import ie.events.OfferingAdjustedIE;
import ie.events.OfferingChangedIE;
import ie.events.PayoutAccountAddedIE;
import ie.events.ProviderListingStoppedIE;
import ie.events.TeamMemberJoinedIE;
import ie.events.TeamMemberLeftIE;
import ie.events.TeamMemberRestoredIE;
import ie.events.TeamMemberRoleChangedIE;
import ie.events.ToolAddedIE;
import ie.events.ToolRetiredIE;
import java.time.Duration;
import kernel.Percentage;
import offering.domain.HelperRange;
import offering.domain.HoursRange;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.support.OfferingWorld;
import org.junit.jupiter.api.Test;

class OfferingEventHandlersTest {

    private final OfferingWorld world = new OfferingWorld();

    private static HomaValuesChangedIE homaHours(int min, int max) {
        Duration d = Duration.ofHours(1);
        Percentage p = Percentage.of(10);
        return new HomaValuesChangedIE(2, p, min, max, d, d, d, d, d, 300, d, p, d, d, d, d, d, d, d, d, p, p, p, p, p, d, 3, NOW);
    }

    @Test
    void aRegisteredBusinessGetsAnEmptyOfferingOnce() {
        HandleBusinessRegisteredIE handler = new HandleBusinessRegisteredIE(world.offerings);

        handler.handle(new BusinessRegisteredIE(PROVIDER, "MALAY", NOW));
        handler.handle(new BusinessRegisteredIE(PROVIDER, "MALAY", NOW));

        assertThat(world.offering().listing()).isEqualTo(Listing.DRAFT);
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(1);
    }

    @Test
    void addedJobsAndToolsCanBePicked() {
        new HandleJobAddedIE(world.catalogues).handle(new JobAddedIE("job-ironing", "Ironing", "group-1", NOW));
        new HandleToolAddedIE(world.catalogues).handle(new ToolAddedIE("tool-iron", "Iron", NOW));

        assertThat(world.catalogue.jobIds()).contains("job-ironing", KITCHEN);
        assertThat(world.catalogue.toolIds()).contains("tool-iron", VACUUM);
        assertThat(world.catalogueSaves).isEqualTo(2);
    }

    @Test
    void aRetiredJobComesOffEveryListingThatOffersItAndTheProviderIsTold() {
        world.liveProvider();
        Offering other = Offering.startFor("provider-2");
        world.offerings.save(other);

        new HandleJobRetiredIE(world.catalogues, world.offerings, world.publisher, world.clock)
                .handle(new JobRetiredIE(LAUNDRY, "No demand", NOW));

        assertThat(world.catalogue.jobIds()).containsExactly(KITCHEN);
        assertThat(world.offering().jobIds()).containsExactly(KITCHEN);
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(2);
        assertThat(world.offerings.timesSaved("provider-2")).isEqualTo(1);
        assertThat(world.publisher.published(OfferingChangedIE.class)).singleElement()
                .satisfies(changed -> assertThat(changed.jobIds()).containsExactly(KITCHEN));
        assertThat(world.publisher.published(OfferingAdjustedIE.class))
                .containsExactly(new OfferingAdjustedIE(PROVIDER, "JOB_RETIRED", NOW));
        assertThat(world.publisher.published(ProviderListingStoppedIE.class)).isEmpty();
    }

    @Test
    void aProviderLeftWithNoJobIsAnnouncedAsStopped() {
        world.liveProvider();
        HandleJobRetiredIE handler =
                new HandleJobRetiredIE(world.catalogues, world.offerings, world.publisher, world.clock);

        handler.handle(new JobRetiredIE(LAUNDRY, "No demand", NOW));
        handler.handle(new JobRetiredIE(KITCHEN, "No demand", NOW));

        assertThat(world.offering().listing()).isEqualTo(Listing.OFFLINE);
        assertThat(world.publisher.published(ProviderListingStoppedIE.class))
                .containsExactly(new ProviderListingStoppedIE(PROVIDER, "NO_JOB", NOW));
    }

    @Test
    void aRetiredToolComesOffEveryListingThatBringsIt() {
        world.liveProvider();
        HandleToolRetiredIE handler =
                new HandleToolRetiredIE(world.catalogues, world.offerings, world.publisher, world.clock);

        handler.handle(new ToolRetiredIE(MOP, "Unused", NOW));
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(1);
        assertThat(world.catalogue.toolIds()).containsExactly(VACUUM);

        handler.handle(new ToolRetiredIE(VACUUM, "Unused", NOW));
        assertThat(world.offering().toolIds()).isEmpty();
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(2);
        assertThat(world.publisher.published(OfferingAdjustedIE.class))
                .containsExactly(new OfferingAdjustedIE(PROVIDER, "TOOL_RETIRED", NOW));
    }

    @Test
    void aNarrowedHomaRangeShrinksHoursAndTheProviderIsTold() {
        world.liveProvider();
        HandleHomaValuesChangedIE handler =
                new HandleHomaValuesChangedIE(world.homaHours, world.offerings, world.publisher, world.clock);

        handler.handle(homaHours(1, 8));
        assertThat(world.announcedHours).contains(new HoursRange(1, 8));
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(1);
        assertThat(world.publisher.published()).isEmpty();

        handler.handle(homaHours(3, 4));
        assertThat(world.offering().state().hours()).isEqualTo(new HoursRange(3, 4));
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(2);
        assertThat(world.publisher.published(OfferingAdjustedIE.class))
                .containsExactly(new OfferingAdjustedIE(PROVIDER, "HOURS_NARROWED", NOW));

        handler.handle(homaHours(6, 6));
        assertThat(world.publisher.published(ProviderListingStoppedIE.class))
                .containsExactly(new ProviderListingStoppedIE(PROVIDER, "NO_HOURS", NOW));
    }

    @Test
    void aLapsedBusinessIsPausedAndResumesWhenVerifiedAgain() {
        world.liveProvider();

        new HandleBusinessLapsedIE(world.offerings, world.publisher, world.clock).handle(new BusinessLapsedIE(PROVIDER, NOW));
        assertThat(world.offering().listing()).isEqualTo(Listing.PAUSED);
        assertThat(world.publisher.published(ProviderListingStoppedIE.class))
                .containsExactly(new ProviderListingStoppedIE(PROVIDER, "BUSINESS_LAPSED", NOW));

        new HandleBusinessVerifiedIE(world.offerings, world.publisher, world.clock)
                .handle(new BusinessVerifiedIE(PROVIDER, "2024", "Bersih Sdn Bhd", NOW, NOW, NOW));
        assertThat(world.offering().isLive()).isTrue();
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(3);
        assertThat(world.publisher.published(OfferingChangedIE.class)).hasSize(2).last()
                .satisfies(changed -> assertThat(changed.listing()).isEqualTo("LIVE"));
    }

    @Test
    void aSeriouslyFlaggedBusinessIsPausedAndResumesOnlyIfClearedAsVerified() {
        world.liveProvider();
        HandleBusinessFlagClearedIE cleared =
                new HandleBusinessFlagClearedIE(world.offerings, world.publisher, world.clock);

        new HandleBusinessSeriouslyFlaggedIE(world.offerings, world.publisher, world.clock)
                .handle(new BusinessSeriouslyFlaggedIE(PROVIDER, NOW));
        assertThat(world.offering().listing()).isEqualTo(Listing.PAUSED);

        cleared.handle(new BusinessFlagClearedIE(PROVIDER, false, NOW));
        assertThat(world.offering().listing()).isEqualTo(Listing.PAUSED);
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(2);

        cleared.handle(new BusinessFlagClearedIE(PROVIDER, true, NOW));
        assertThat(world.offering().isLive()).isTrue();
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(3);
    }

    @Test
    void theTeamIsCountedFromTheVerificationEvents() {
        world.newProvider();

        new HandleTeamMemberJoinedIE(world.offerings, world.publisher, world.clock)
                .handle(new TeamMemberJoinedIE("m-1", PROVIDER, "HELPER", "Siti", "p.jpg", NOW, NOW));
        new HandleTeamMemberRestoredIE(world.offerings, world.publisher, world.clock)
                .handle(new TeamMemberRestoredIE("m-2", PROVIDER, "HELPER", "Aminah", "p.jpg", NOW, NOW));
        assertThat(world.offering().state().team().helpers()).isEqualTo(2);

        new HandleTeamMemberRoleChangedIE(world.offerings, world.publisher, world.clock)
                .handle(new TeamMemberRoleChangedIE("m-2", PROVIDER, "SUPERVISOR", NOW));
        assertThat(world.offering().state().team().helpers()).isEqualTo(1);
        assertThat(world.offering().state().team().supervisors()).isEqualTo(1);

        new HandleTeamMemberLeftIE(world.offerings, world.publisher, world.clock)
                .handle(new TeamMemberLeftIE("m-2", PROVIDER, "REMOVED", NOW));
        assertThat(world.offering().state().team().supervisors()).isZero();
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(5);
    }

    @Test
    void aLiveProviderWhoseLastHelperLeavesIsAnnouncedAsStopped() {
        world.liveProvider();
        HandleTeamMemberLeftIE handler = new HandleTeamMemberLeftIE(world.offerings, world.publisher, world.clock);

        handler.handle(new TeamMemberLeftIE("helper-2", PROVIDER, "LAPSED", NOW));
        assertThat(world.offering().state().helpers()).isEqualTo(new HelperRange(1, 1));
        assertThat(world.publisher.published(OfferingChangedIE.class)).singleElement()
                .satisfies(changed -> assertThat(changed.maxHelpers()).isEqualTo(1));

        handler.handle(new TeamMemberLeftIE("helper-1", PROVIDER, "REMOVED", NOW));
        assertThat(world.publisher.published(ProviderListingStoppedIE.class))
                .containsExactly(new ProviderListingStoppedIE(PROVIDER, "NO_HELPER", NOW));
    }

    @Test
    void anAddedPayoutAccountCountsTowardsGoingLive() {
        world.newProvider();

        new HandlePayoutAccountAddedIE(world.offerings, world.publisher, world.clock)
                .handle(new PayoutAccountAddedIE(PROVIDER, NOW));

        assertThat(world.offering().state().payoutAccountAdded()).isTrue();
        assertThat(world.offerings.timesSaved(PROVIDER)).isEqualTo(2);
    }

    @Test
    void eventsAboutAnUnknownProviderAreIgnored() {
        new HandleBusinessVerifiedIE(world.offerings, world.publisher, world.clock)
                .handle(new BusinessVerifiedIE("nobody", "2024", "X", NOW, NOW, NOW));
        new HandleBusinessFlagClearedIE(world.offerings, world.publisher, world.clock)
                .handle(new BusinessFlagClearedIE("nobody", true, NOW));

        assertThat(world.offerings.all()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }
}
