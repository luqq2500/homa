package reservation.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static reservation.support.ReservationFixtures.*;

import ie.events.BusinessFlagClearedIE;
import ie.events.BusinessLapsedIE;
import ie.events.BusinessSeriouslyFlaggedIE;
import ie.events.BusinessVerifiedIE;
import ie.events.CustomerDetailsChangedIE;
import ie.events.CustomerRegisteredIE;
import ie.events.HomaValuesChangedIE;
import ie.events.OfferingChangedIE;
import ie.events.ProviderCalendarChangedIE;
import ie.events.TeamMemberJoinedIE;
import ie.events.TeamMemberRestoredIE;
import ie.events.TeamMemberRoleChangedIE;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kernel.City;
import kernel.GeoPoint;
import kernel.Money;
import kernel.Percentage;
import kernel.TimeRange;
import org.junit.jupiter.api.Test;
import reservation.domain.Address;
import reservation.domain.Customer;
import reservation.domain.ProviderOpenTime;
import reservation.domain.ProviderStanding;
import reservation.domain.ProviderTerms;
import reservation.domain.TeamRole;
import reservation.support.ReservationWorld;

class ReservationProjectionHandlersTest {

    private final ReservationWorld world = new ReservationWorld();

    @Test
    void aChangedOfferingBecomesTheTermsNewRequestsAreMadeWith() {
        new HandleOfferingChangedIE(world.providerTerms).handle(new OfferingChangedIE(PROVIDER, "LIVE", "Bersih", "p.jpg",
                Set.of(KITCHEN, LAUNDRY), Set.of(), 1, 3, 3, 5, Money.ofRinggit(30), true, Money.ofRinggit(10), 2, 5,
                Set.of(City.CYBERJAYA, City.PUTRAJAYA), "STANDARD", "REQUEST_AND_ACCEPT", null, null, NOW));

        assertThat(world.providerTerms.find(PROVIDER)).contains(terms());
    }

    @Test
    void anOfferingStillBeingSetUpIsKeptAsNotLive() {
        new HandleOfferingChangedIE(world.providerTerms).handle(new OfferingChangedIE("provider-2", "DRAFT", null, null,
                Set.of(), Set.of(), 0, 0, 0, 0, null, false, null, 0, 0, Set.of(), null, "INSTANT_BOOK", null, null, NOW));

        ProviderTerms draft = world.providerTerms.find("provider-2").orElseThrow();
        assertThat(draft.live()).isFalse();
        assertThat(draft.cancellationTier()).isNull();
        assertThat(draft.usesInstantBook()).isTrue();
    }

    @Test
    void aChangedCalendarBecomesTheProvidersOpenTime() {
        List<TimeRange> open = List.of(TimeRange.of(START, END));

        new HandleProviderCalendarChangedIE(world.openTimes)
                .handle(new ProviderCalendarChangedIE(PROVIDER, open, Duration.ofMinutes(45), NOW));

        assertThat(world.openTimes.find(PROVIDER)).contains(new ProviderOpenTime(PROVIDER, open, Duration.ofMinutes(45)));
    }

    @Test
    void changedHomaValuesApplyToNewReservations() {
        world.announcedValues = Optional.empty();

        new HandleHomaValuesChangedIE(world.homaValues).handle(new HomaValuesChangedIE(1, Percentage.of(15), 1, 6,
                Duration.ofHours(2), Duration.ofDays(30), Duration.ofHours(6), Duration.ofMinutes(15),
                Duration.ofMinutes(15), 300, Duration.ofMinutes(15), Percentage.of(30), Duration.ofHours(1),
                Duration.ofHours(24), Duration.ofHours(1), Duration.ofMinutes(30), Duration.ofHours(24),
                Duration.ofDays(120), Duration.ofHours(24), Duration.ofHours(24), Percentage.of(10), Percentage.of(20),
                Percentage.of(20), Percentage.of(30), Percentage.of(50), Duration.ofDays(14), 3, NOW));

        assertThat(world.announcedValues).contains(VALUES);
    }

    @Test
    void theBusinesssStandingFollowsTheVerificationEvents() {
        Instant until = NOW.plus(Duration.ofDays(200));
        String business = "provider-2";

        new HandleBusinessVerifiedIE(world.standings).handle(new BusinessVerifiedIE(business, "2024", "X", until, NOW, NOW));
        assertThat(world.standings.find(business)).contains(new ProviderStanding(business, until, false));

        new HandleBusinessSeriouslyFlaggedIE(world.standings, world.reservations, world.publisher, world.clock)
                .handle(new BusinessSeriouslyFlaggedIE(business, NOW));
        assertThat(world.standings.find(business)).contains(new ProviderStanding(business, until, true));

        new HandleBusinessVerifiedIE(world.standings).handle(new BusinessVerifiedIE(business, "2024", "X", until.plusSeconds(1), NOW, NOW));
        assertThat(world.standings.find(business)).contains(new ProviderStanding(business, until.plusSeconds(1), true));

        new HandleBusinessFlagClearedIE(world.standings).handle(new BusinessFlagClearedIE(business, true, NOW));
        assertThat(world.standings.find(business)).contains(new ProviderStanding(business, until.plusSeconds(1), false));

        new HandleBusinessLapsedIE(world.standings).handle(new BusinessLapsedIE(business, NOW));
        assertThat(world.standings.find(business)).contains(new ProviderStanding(business, null, false));
    }

    @Test
    void aBusinessHomaHasNotHeardOfStartsUnverified() {
        new HandleBusinessLapsedIE(world.standings).handle(new BusinessLapsedIE("provider-3", NOW));
        new HandleBusinessFlagClearedIE(world.standings).handle(new BusinessFlagClearedIE("provider-4", true, NOW));

        assertThat(world.standings.find("provider-3")).contains(ProviderStanding.unverified("provider-3"));
        assertThat(world.standings.find("provider-4")).contains(ProviderStanding.unverified("provider-4"));
    }

    @Test
    void theRosterFollowsTheTeamEvents() {
        String business = "provider-2";

        new HandleTeamMemberJoinedIE(world.rosters)
                .handle(new TeamMemberJoinedIE("m-1", business, "HELPER", "Siti", "p.jpg", NOW, NOW));
        new HandleTeamMemberRestoredIE(world.rosters)
                .handle(new TeamMemberRestoredIE("m-2", business, "SUPERVISOR", "Aminah", "p.jpg", NOW, NOW));
        new HandleTeamMemberRoleChangedIE(world.rosters)
                .handle(new TeamMemberRoleChangedIE("m-1", business, "SUPERVISOR", NOW));

        assertThat(world.rosters.find(business).orElseThrow().roleOf("m-1")).contains(TeamRole.SUPERVISOR);
        assertThat(world.rosters.find(business).orElseThrow().roleOf("m-2")).contains(TeamRole.SUPERVISOR);
        assertThat(world.rosters.timesSaved(business)).isEqualTo(3);
    }

    @Test
    void registeredAndChangedCustomersAreKnownForNewRequests() {
        GeoPoint pin = new GeoPoint(2.93, 101.69);

        new HandleCustomerRegisteredIE(world.customers).handle(new CustomerRegisteredIE("customer-2", "Farah",
                "3 Presint 8", pin, City.PUTRAJAYA, "", "MALAY", NOW));
        assertThat(world.customers.find("customer-2")).contains(
                new Customer("customer-2", "Farah", new Address("3 Presint 8", pin, City.PUTRAJAYA), ""));

        new HandleCustomerDetailsChangedIE(world.customers).handle(new CustomerDetailsChangedIE("customer-2",
                "Farah Aziz", "3 Presint 8", pin, City.PUTRAJAYA, "Gate B", "MALAY", NOW));
        assertThat(world.customers.find("customer-2")).contains(
                new Customer("customer-2", "Farah Aziz", new Address("3 Presint 8", pin, City.PUTRAJAYA), "Gate B"));
    }
}
