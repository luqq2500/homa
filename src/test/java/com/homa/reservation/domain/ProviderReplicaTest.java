package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HELPERS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HOURS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_RATE;
import static com.homa.fixtures.HomaFixtures.FLAT_TRAVEL_FEE;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.fixtures.HomaFixtures.JOB_BUFFER;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_CITIES;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.MINIMUM_NOTICE;
import static com.homa.reservation.domain.ReservationFixtures.AZMAN_PERSON_ID;
import static com.homa.reservation.domain.ReservationFixtures.BATHROOM_CLEANING;
import static com.homa.reservation.domain.ReservationFixtures.BOOKING_STEP;
import static com.homa.reservation.domain.ReservationFixtures.KITCHEN_CLEANING;
import static com.homa.reservation.domain.ReservationFixtures.LAUNDRY_FOLDING;
import static com.homa.reservation.domain.ReservationFixtures.deactivated;
import static com.homa.reservation.domain.ReservationFixtures.farid;
import static com.homa.reservation.domain.ReservationFixtures.kemasHome;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.kemasWorkingHours;
import static com.homa.reservation.domain.ReservationFixtures.nurul;
import static com.homa.reservation.domain.ReservationFixtures.rina;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.reservation.domain.replica.BookingBounds;
import com.homa.reservation.domain.replica.OfferedTerms;
import com.homa.reservation.domain.replica.ProviderListing;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.replica.TravelFeeRule;
import com.homa.reservation.domain.replica.WorkingHours;
import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import com.homa.reservation.domain.terms.CancellationTier;
import com.homa.reservation.domain.terms.DiscountRule;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderReplicaTest {

    private static final ProviderListing KEMAS_LISTING = new ProviderListing(
            Set.copyOf(KEMAS_HOME_CITIES),
            Set.of(KITCHEN_CLEANING, LAUNDRY_FOLDING, BATHROOM_CLEANING),
            new OfferedTerms(HOURLY_RATE_PER_HELPER, TravelFeeRule.flat(FLAT_TRAVEL_FEE),
                    new DiscountRule(DISCOUNT_RATE, DISCOUNT_MIN_HOURS, DISCOUNT_MIN_HELPERS), AcceptancePolicy.REQUEST,
                    CancellationTier.STANDARD),
            new BookingBounds(Duration.ofHours(2), Duration.ofHours(6), BOOKING_STEP, 1, Integer.MAX_VALUE),
            MINIMUM_NOTICE,
            JOB_BUFFER);

    /** Siti, Nurul and Rina are active helpers; Farid supervises. */
    private static final List<RosterableMember> KEMAS_TEAM = List.of(siti(), nurul(), rina(), farid());

    @Test
    @Tag("PRV-2")
    @DisplayName("Before Listing publishes Kemas Home's terms, it can't be booked, and it works no hours.")
    void providerHeardOfOnlyByNameCannotBeBooked() {
        ProviderReplica unheard = ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID);

        assertThat(unheard).isEqualTo(new ProviderReplica(KEMAS_HOME_PROVIDER_ID, Optional.empty(), false,
                Optional.empty(), new WorkingHours(Map.of())));
        assertThat(unheard.asBookable(KEMAS_TEAM)).isEmpty();
        assertThat(unheard.ownedBy(AZMAN_PERSON_ID).withBookability(true).working(kemasWorkingHours())
                .asBookable(KEMAS_TEAM)).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home went live and Listing published its terms and hours, in any order: it can be booked.")
    void listedLiveProviderIsBookableWhicheverEventCameFirst() {
        ProviderReplica listedFirst = ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID).ownedBy(AZMAN_PERSON_ID)
                .listedAs(KEMAS_LISTING).working(kemasWorkingHours()).withBookability(true);
        ProviderReplica liveFirst = ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID)
                .withBookability(true).working(kemasWorkingHours()).listedAs(KEMAS_LISTING).ownedBy(AZMAN_PERSON_ID);

        assertThat(listedFirst).isEqualTo(liveFirst);
        assertThat(listedFirst.asBookable(KEMAS_TEAM)).contains(kemasHome());
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home stops being bookable and keeps its terms and hours for when it is bookable again.")
    void providerThatStopsBeingBookableKeepsItsListing() {
        ProviderReplica live = ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID).ownedBy(AZMAN_PERSON_ID)
                .listedAs(KEMAS_LISTING).working(kemasWorkingHours()).withBookability(true);

        ProviderReplica stopped = live.withBookability(false);

        assertThat(stopped.asBookable(KEMAS_TEAM)).contains(kemasHomeProvider().notBookable().build());
        assertThat(stopped.withBookability(true)).isEqualTo(live);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Azman opened Kemas Home: the replica holds its owner, so the owner can act on its reservations, whichever event came first.")
    void providerKeepsItsOwnerWhicheverEventCameFirst() {
        ProviderReplica opened = ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID).ownedBy(AZMAN_PERSON_ID);

        assertThat(opened.ownerPersonId()).contains(AZMAN_PERSON_ID);
        assertThat(ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID).ownerPersonId()).isEmpty();
        assertThat(opened.listedAs(KEMAS_LISTING).working(kemasWorkingHours()).withBookability(true)
                .asBookable(KEMAS_TEAM).orElseThrow().ownerPersonId()).isEqualTo(AZMAN_PERSON_ID);
        assertThat(opened.bookable()).isFalse();
        assertThat(opened.listing()).isEmpty();
        assertThat(opened.workingHours()).isEqualTo(new WorkingHours(Map.of()));
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Kemas Home raises its rate to RM30: the replica holds the new listing, which new bookings use.")
    void newListingReplacesTheOldOne() {
        ProviderListing raised = new ProviderListing(KEMAS_LISTING.cities(), KEMAS_LISTING.jobs(),
                new OfferedTerms(com.homa.kernel.Money.myr(30), TravelFeeRule.flat(FLAT_TRAVEL_FEE),
                        KEMAS_LISTING.terms().discount(), AcceptancePolicy.REQUEST, CancellationTier.FIRM),
                KEMAS_LISTING.bounds(), KEMAS_LISTING.minimumNotice(), KEMAS_LISTING.buffer());
        ProviderReplica live = ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID).ownedBy(AZMAN_PERSON_ID)
                .listedAs(KEMAS_LISTING).working(kemasWorkingHours()).withBookability(true);

        ProviderReplica changed = live.listedAs(raised);

        assertThat(changed).isEqualTo(new ProviderReplica(KEMAS_HOME_PROVIDER_ID,
                Optional.of(AZMAN_PERSON_ID), true, Optional.of(raised), kemasWorkingHours()));
        assertThat(changed.working(new WorkingHours(Map.of())).workingHours()).isEqualTo(new WorkingHours(Map.of()));
        assertThat(changed.working(new WorkingHours(Map.of())).listing()).contains(raised);
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Only the provider's own active helpers count: not a supervisor, a deactivated helper or another provider's.")
    void activeHelpersAreCountedFromTheProvidersOwnActiveHelpers() {
        ProviderReplica live = ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID).ownedBy(AZMAN_PERSON_ID)
                .listedAs(KEMAS_LISTING).working(kemasWorkingHours()).withBookability(true);
        RosterableMember elsewhere = RosterableMember.local("member-elsewhere", "person-hana",
                "provider-another", MemberRole.HELPER, "Hana", "photo-hana", true);

        assertThat(live.asBookable(List.of(siti(), deactivated(nurul()), farid(), elsewhere)))
                .contains(kemasHomeProvider().activeHelpers(1).build());
        assertThat(live.asBookable(List.of()))
                .contains(kemasHomeProvider().activeHelpers(0).build());
    }
}
