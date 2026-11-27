package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.MINIMUM_NOTICE;
import static com.homa.listing.domain.ListingFixtures.instantListing;
import static com.homa.listing.domain.ListingFixtures.kemasHomeListing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.listing.domain.exception.ListingRuleViolationException;
import com.homa.listing.domain.exception.NoticeBelowFloorException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-6")
class ProviderListingMinimumNoticeTest {

    @Test
    @DisplayName("Kemas Home (Request) can't go below 2 hours. An Instant provider at 1 hour that switches to Request "
            + "moves to 2 hours and is told. A larger team sets 24 hours because it plans the day before.")
    void switchingToRequestRaisesAOneHourNoticeToTwoHours() {
        ProviderListing instant = instantListing();
        instant.setMinimumNotice(MinimumNotice.of(Duration.ofHours(1)));

        ListingTermsChange change = instant.choosePolicy(AcceptancePolicy.REQUEST);

        assertThat(change.noticeRaised()).isTrue();
        assertThat(change.terms().notice().duration()).isEqualTo(Duration.ofHours(2));
        assertThat(change.terms().policy()).isEqualTo(AcceptancePolicy.REQUEST);
        assertThat(instant.terms()).isEqualTo(change.terms());
    }

    @Test
    @DisplayName("Kemas Home (Request) can't go below 2 hours")
    void aRequestProviderKeepsTwoHoursAndCantGoLower() {
        ProviderListing kemasHome = kemasHomeListing();

        assertThat(kemasHome.terms().notice().duration()).isEqualTo(MINIMUM_NOTICE);
        assertThatThrownBy(() -> kemasHome.setMinimumNotice(MinimumNotice.of(Duration.ofMinutes(119))))
                .isInstanceOf(NoticeBelowFloorException.class)
                .extracting(e -> ((ListingRuleViolationException) e).ruleId()).isEqualTo("OFR-6");
        assertThat(kemasHome.terms().notice().duration()).isEqualTo(MINIMUM_NOTICE);
        assertThat(kemasHome.setMinimumNotice(MinimumNotice.of(Duration.ofHours(2))).noticeRaised()).isFalse();
    }

    @Test
    @DisplayName("An Instant provider can set exactly 1 hour, but not 59 minutes")
    void anInstantProviderCanGoDownToOneHour() {
        ProviderListing instant = instantListing();

        assertThat(instant.setMinimumNotice(MinimumNotice.of(Duration.ofHours(1))).terms().notice().duration())
                .isEqualTo(Duration.ofHours(1));
        assertThatThrownBy(() -> instant.setMinimumNotice(MinimumNotice.of(Duration.ofMinutes(59))))
                .isInstanceOf(NoticeBelowFloorException.class);
    }

    @Test
    @DisplayName("A larger team sets 24 hours; switching it to Request keeps 24 hours and nobody is notified")
    void switchingToRequestKeepsANoticeAlreadyAtOrAboveTwoHours() {
        ProviderListing largerTeam = instantListing();
        largerTeam.setMinimumNotice(MinimumNotice.of(Duration.ofHours(24)));

        ListingTermsChange change = largerTeam.choosePolicy(AcceptancePolicy.REQUEST);

        assertThat(change.noticeRaised()).isFalse();
        assertThat(change.terms().notice().duration()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    @DisplayName("Switching from Request to Instant keeps the notice as it is")
    void switchingToInstantKeepsTheNotice() {
        ListingTermsChange change = kemasHomeListing().choosePolicy(AcceptancePolicy.INSTANT);

        assertThat(change.noticeRaised()).isFalse();
        assertThat(change.terms().notice().duration()).isEqualTo(MINIMUM_NOTICE);
        assertThat(change.terms().policy()).isEqualTo(AcceptancePolicy.INSTANT);
    }
}
