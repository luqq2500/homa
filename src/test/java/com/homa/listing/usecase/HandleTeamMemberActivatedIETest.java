package com.homa.listing.usecase;

import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.NOW;
import static com.homa.listing.usecase.ListingTestBed.helperActivated;
import static com.homa.listing.usecase.ListingTestBed.supervisorActivated;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.kernel.Money;
import com.homa.listing.domain.ProviderStanding;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-5")
class HandleTeamMemberActivatedIETest {

    private final ListingTestBed bed = new ListingTestBed();

    @Test
    @DisplayName("Siti, Nurul and Rina are active helpers before Kemas Home has a listing: they are counted, nothing "
            + "is published")
    void helpersAreCountedBeforeTheListingExists() {
        bed.memberActivated.handle(helperActivated("team-event-1", SITI_MEMBER_ID));
        bed.memberActivated.handle(helperActivated("team-event-2", NURUL_MEMBER_ID));
        bed.memberActivated.handle(helperActivated("team-event-3", RINA_MEMBER_ID));

        ProviderStanding standing = bed.standings.findById(KEMAS_HOME_ID).orElseThrow();
        assertThat(standing.activeHelperIds()).containsExactlyInAnyOrder(SITI_MEMBER_ID, NURUL_MEMBER_ID, RINA_MEMBER_ID);
        assertThat(standing.isListed()).isFalse();
        assertThat(bed.processed.handled("team-event-1", "listing.TeamMemberActivated")).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("Rina becomes active after the listing exists: customers can now book up to 3 helpers, so the full "
            + "terms are published again")
    void aNewHelperRepublishesTheTermsWithTheNewMaximum() {
        bed.memberActivated.handle(helperActivated("team-event-1", SITI_MEMBER_ID));
        bed.memberActivated.handle(helperActivated("team-event-2", NURUL_MEMBER_ID));
        bed.withKemasHomeListing();

        bed.memberActivated.handle(helperActivated("team-event-3", RINA_MEMBER_ID));

        assertThat(bed.publisher.single(ListingTermsChangedIE.class)).isEqualTo(new ListingTermsChangedIE(
                "listing-event-2", KEMAS_HOME,
                List.of("CYBERJAYA", "PUTRAJAYA", "SALAK_TINGGI"),
                List.of("BATHROOM_CLEANING", "KITCHEN_CLEANING", "LAUNDRY_FOLDING"),
                Money.myr(25), "NONE", null, Map.of(), null, null, 0,
                Duration.ofHours(1), Duration.ofHours(8), Duration.ofMinutes(15), 1, 3,
                Duration.ofHours(2), Duration.ofMinutes(30), "REQUEST", "STANDARD", false, NOW));
    }

    @Test
    @DisplayName("Supervisor Farid doesn't count and changes nothing customers book, so nothing is published")
    void aSupervisorIsNotCounted() {
        bed.withKemasHomeListing();

        bed.memberActivated.handle(supervisorActivated("team-event-1", FARID_MEMBER_ID));

        assertThat(bed.standings.findById(KEMAS_HOME_ID).orElseThrow().activeHelperCount()).isZero();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("Siti activated again under another event is still one helper, and nothing is published")
    void aHelperActivatedAgainIsCountedOnce() {
        bed.withKemasHomeListing();
        bed.memberActivated.handle(helperActivated("team-event-1", SITI_MEMBER_ID));
        bed.publisher.clear();

        bed.memberActivated.handle(helperActivated("team-event-2", SITI_MEMBER_ID));

        assertThat(bed.standings.findById(KEMAS_HOME_ID).orElseThrow().activeHelperCount()).isEqualTo(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A redelivered TeamMemberActivatedIE changes nothing and publishes nothing")
    void aRedeliveryChangesNothing() {
        bed.withKemasHomeListing();
        bed.memberActivated.handle(helperActivated("team-event-1", SITI_MEMBER_ID));
        bed.standings.save(ProviderStanding.unknown(KEMAS_HOME_ID));
        bed.publisher.clear();

        bed.memberActivated.handle(helperActivated("team-event-1", SITI_MEMBER_ID));

        assertThat(bed.standings.findById(KEMAS_HOME_ID).orElseThrow().activeHelperCount()).isZero();
        assertThat(bed.publisher.published()).isEmpty();
    }
}
