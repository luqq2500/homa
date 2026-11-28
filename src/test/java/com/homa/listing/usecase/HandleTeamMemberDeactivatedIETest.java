package com.homa.listing.usecase;

import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.NOW;
import static com.homa.listing.usecase.ListingTestBed.helperActivated;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeBookability;
import static com.homa.listing.usecase.ListingTestBed.memberDeactivated;
import static com.homa.listing.usecase.ListingTestBed.memberRemoved;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.kernel.Money;
import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.usecase.dto.ProviderSetBookingRulesRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleTeamMemberDeactivatedIETest {

    private static final String HANDLER = "listing.TeamMemberDeactivated";

    /** Siti, Nurul and Rina are active helpers and Farid is an active supervisor; Kemas Home's listing exists. */
    private final ListingTestBed bed = new ListingTestBed().withKemasHomeTeam().withKemasHomeListing();

    /** Kemas Home's terms as the setup creates them, with the maximum of helpers customers can book now. */
    private static ListingTermsChangedIE kemasHomeTerms(String eventId, int maxHelpers) {
        return new ListingTermsChangedIE(eventId, KEMAS_HOME,
                List.of("CYBERJAYA", "PUTRAJAYA", "SALAK_TINGGI"),
                List.of("BATHROOM_CLEANING", "KITCHEN_CLEANING", "LAUNDRY_FOLDING"),
                Money.myr(25), "NONE", null, Map.of(), null, null, 0,
                Duration.ofHours(1), Duration.ofHours(8), Duration.ofMinutes(15), 1, maxHelpers,
                Duration.ofHours(2), Duration.ofMinutes(30), "REQUEST", "STANDARD", false, NOW);
    }

    private int activeHelpers() {
        return bed.standings.findById(KEMAS_HOME_ID).orElseThrow().activeHelperCount();
    }

    @Test
    @Tag("HLP-5")
    @Tag("OFR-5")
    @DisplayName("Siti is deactivated for two weeks' leave and reactivated when she's back: customers book up to 2 "
            + "helpers while she is away, and up to 3 again when she is back")
    void aDeactivatedHelperLowersTheMaximumUntilSheIsBack() {
        bed.memberDeactivated.handle(memberDeactivated("team-event-1", SITI_MEMBER_ID));

        assertThat(bed.publisher.single(ListingTermsChangedIE.class)).isEqualTo(kemasHomeTerms("listing-event-2", 2));
        assertThat(activeHelpers()).isEqualTo(2);
        assertThat(bed.processed.handled("team-event-1", HANDLER)).isTrue();
        bed.publisher.clear();

        bed.memberActivated.handle(helperActivated("team-event-2", SITI_MEMBER_ID));

        assertThat(bed.publisher.single(ListingTermsChangedIE.class)).isEqualTo(kemasHomeTerms("listing-event-3", 3));
        assertThat(activeHelpers()).isEqualTo(3);
    }

    @Test
    @Tag("HLP-6")
    @Tag("OFR-5")
    @DisplayName("Nurul, deactivated on Thursday, can't be rostered, so customers can't book more helpers than are "
            + "active: a maximum of 3 becomes 2")
    void anExplicitMaximumNeverExceedsTheHelpersLeft() {
        bed.setBookingRules.set(new ProviderSetBookingRulesRequest(AZMAN, KEMAS_HOME, 60, 480, 1, 3, null, 30,
                "REQUEST", "STANDARD"));
        assertThat(bed.publisher.single(ListingTermsChangedIE.class).maxHelpers()).isEqualTo(3);
        bed.publisher.clear();

        bed.memberDeactivated.handle(memberDeactivated("team-event-1", NURUL_MEMBER_ID));

        assertThat(bed.publisher.single(ListingTermsChangedIE.class)).isEqualTo(kemasHomeTerms("listing-event-3", 2));
        assertThat(bed.standings.findById(KEMAS_HOME_ID).orElseThrow().activeHelperIds())
                .containsExactlyInAnyOrder(SITI_MEMBER_ID, RINA_MEMBER_ID);
    }

    @Test
    @Tag("HLP-8")
    @Tag("OFR-5")
    @DisplayName("Rina resigns and is removed the same day. If she returns a year later, she's added and verified as "
            + "new: she is counted again only under her new membership")
    void aRemovedHelperStopsCountingAndOnlyANewMemberCountsAgain() {
        bed.memberDeactivated.handle(memberRemoved("team-event-1", RINA_MEMBER_ID));

        assertThat(bed.publisher.single(ListingTermsChangedIE.class)).isEqualTo(kemasHomeTerms("listing-event-2", 2));
        assertThat(bed.standings.findById(KEMAS_HOME_ID).orElseThrow().activeHelperIds())
                .containsExactlyInAnyOrder(SITI_MEMBER_ID, NURUL_MEMBER_ID);
        bed.publisher.clear();

        bed.memberActivated.handle(helperActivated("team-event-2", "member-rina-returned"));

        assertThat(bed.publisher.single(ListingTermsChangedIE.class)).isEqualTo(kemasHomeTerms("listing-event-3", 3));
        assertThat(bed.standings.findById(KEMAS_HOME_ID).orElseThrow().activeHelperIds())
                .containsExactlyInAnyOrder(SITI_MEMBER_ID, NURUL_MEMBER_ID, "member-rina-returned");
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Supervisor Farid doesn't count: deactivating him changes nothing customers book, so nothing is "
            + "published")
    void aSupervisorChangesNothing() {
        bed.memberDeactivated.handle(memberDeactivated("team-event-1", FARID_MEMBER_ID));

        assertThat(activeHelpers()).isEqualTo(3);
        assertThat(bed.processed.handled("team-event-1", HANDLER)).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("A member Listing never counted changes nothing, so nothing is published")
    void aMemberThatWasNeverCountedChangesNothing() {
        bed.memberDeactivated.handle(memberDeactivated("team-event-1", "member-never-activated"));

        assertThat(activeHelpers()).isEqualTo(3);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("Siti is deactivated before Kemas Home has a listing: she stops counting, and nothing is published")
    void aHelperIsUncountedBeforeTheListingExists() {
        ListingTestBed noListing = new ListingTestBed().withKemasHomeTeam();

        noListing.memberDeactivated.handle(memberDeactivated("team-event-1", SITI_MEMBER_ID));

        assertThat(noListing.standings.findById(KEMAS_HOME_ID).orElseThrow().activeHelperIds())
                .containsExactlyInAnyOrder(NURUL_MEMBER_ID, RINA_MEMBER_ID);
        assertThat(noListing.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("A deactivation for a provider Listing has heard nothing about counts nobody and publishes nothing")
    void aDeactivationForAnUnknownProviderChangesNothing() {
        ListingTestBed unknown = new ListingTestBed();

        unknown.memberDeactivated.handle(memberDeactivated("team-event-1", SITI_MEMBER_ID));

        ProviderStanding standing = unknown.standings.findById(KEMAS_HOME_ID).orElseThrow();
        assertThat(standing.activeHelperCount()).isZero();
        assertThat(standing.isListed()).isFalse();
        assertThat(unknown.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-5")
    @Tag("PRV-2")
    @DisplayName("When all its helpers are deactivated for a holiday week, Kemas Home disappears from search: the count "
            + "falls to 0, but it leaves search only when Provider says it is no longer bookable")
    void whetherTheProviderStaysBookableIsProvidersDecision() {
        ListingTestBed live = new ListingTestBed().withKemasHomeTeam().withKemasHomeLive().withKemasHomeListing();

        live.memberDeactivated.handle(memberDeactivated("team-event-1", SITI_MEMBER_ID));
        live.memberDeactivated.handle(memberDeactivated("team-event-2", NURUL_MEMBER_ID));
        live.memberDeactivated.handle(memberDeactivated("team-event-3", RINA_MEMBER_ID));

        assertThat(live.publisher.published()).containsExactly(
                kemasHomeTerms("listing-event-2", 2), kemasHomeTerms("listing-event-3", 1),
                kemasHomeTerms("listing-event-4", 0));
        ProviderStanding standing = live.standings.findById(KEMAS_HOME_ID).orElseThrow();
        assertThat(standing.activeHelperCount()).isZero();
        assertThat(standing.live()).isTrue();
        assertThat(standing.bookable()).isTrue();
        assertThat(standing.isListed()).isTrue();

        live.bookabilityChanged.handle(kemasHomeBookability("provider-event-2", false, "NO_ACTIVE_HELPERS"));

        assertThat(live.standings.findById(KEMAS_HOME_ID).orElseThrow().isListed()).isFalse();
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("Rina is deactivated and then removed: she was already out, so the removal changes nothing and "
            + "publishes nothing")
    void aMemberAlreadyOutChangesNothingWhenDeactivatedAgain() {
        bed.memberDeactivated.handle(memberDeactivated("team-event-1", RINA_MEMBER_ID));
        bed.publisher.clear();

        bed.memberDeactivated.handle(memberRemoved("team-event-2", RINA_MEMBER_ID));

        assertThat(activeHelpers()).isEqualTo(2);
        assertThat(bed.processed.handled("team-event-2", HANDLER)).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("A redelivered TeamMemberDeactivatedIE changes nothing and publishes nothing")
    void aRedeliveryChangesNothing() {
        bed.memberDeactivated.handle(memberDeactivated("team-event-1", SITI_MEMBER_ID));
        bed.memberActivated.handle(helperActivated("team-event-2", SITI_MEMBER_ID));
        bed.publisher.clear();

        bed.memberDeactivated.handle(memberDeactivated("team-event-1", SITI_MEMBER_ID));

        assertThat(activeHelpers()).isEqualTo(3);
        assertThat(bed.publisher.published()).isEmpty();
    }
}
