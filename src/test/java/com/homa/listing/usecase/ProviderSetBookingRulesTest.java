package com.homa.listing.usecase;

import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.HANA;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeCreation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.kernel.Money;
import com.homa.listing.domain.AcceptancePolicy;
import com.homa.listing.domain.CancellationTier;
import com.homa.listing.domain.ListingTerms;
import com.homa.listing.domain.exception.InvalidBookingBoundsException;
import com.homa.listing.domain.exception.NoticeBelowFloorException;
import com.homa.listing.usecase.RecordingProviderNotifier.NoticeRaised;
import com.homa.listing.usecase.dto.ProviderSetBookingRulesRequest;
import com.homa.listing.usecase.dto.ProviderSetBookingRulesResponse;
import com.homa.listing.usecase.exception.NotAllowedException;
import java.time.Duration;
import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderSetBookingRulesTest {

    private final ListingTestBed bed = new ListingTestBed().withKemasHomeTeam().withKemasHomeListing();

    @Test
    @Tag("OFR-5")
    @DisplayName("Kemas Home sets 2 to 6 hours and keeps the helper default; with 3 active helpers, customers book 1 to 3")
    void kemasHomeSetsItsHoursAndKeepsTheHelperDefault() {
        ProviderSetBookingRulesResponse response = bed.setBookingRules.set(
                rules(AZMAN, 120, 360, 1, null, 120L, 30, "REQUEST", "STANDARD"));

        assertThat(response).isEqualTo(new ProviderSetBookingRulesResponse(KEMAS_HOME, 120, false));
        ListingTerms stored = bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow();
        assertThat(stored.bounds().minHours()).isEqualTo(Duration.ofHours(2));
        assertThat(stored.bounds().maxHours()).isEqualTo(Duration.ofHours(6));
        assertThat(stored.bounds().maxHelpers()).isEqualTo(OptionalInt.empty());
        ListingTermsChangedIE event = bed.publisher.single(ListingTermsChangedIE.class);
        assertThat(event.minBookingHours()).isEqualTo(Duration.ofHours(2));
        assertThat(event.maxBookingHours()).isEqualTo(Duration.ofHours(6));
        assertThat(event.bookingStep()).isEqualTo(Duration.ofMinutes(15));
        assertThat(event.minHelpers()).isEqualTo(1);
        assertThat(event.maxHelpers()).isEqualTo(3);
        assertThat(event.noticeRaised()).isFalse();
        assertThat(bed.notifier.told()).isEmpty();
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("An explicit maximum of 2 helpers is kept; a maximum of 5 still can't book more than the 3 active")
    void anExplicitMaximumNeverExceedsTheActiveHelpers() {
        bed.setBookingRules.set(rules(AZMAN, 60, 480, 2, 2, null, 30, "REQUEST", "STANDARD"));

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow().bounds().maxHelpers()).isEqualTo(OptionalInt.of(2));
        ListingTermsChangedIE twoHelpers = bed.publisher.single(ListingTermsChangedIE.class);
        assertThat(twoHelpers.minHelpers()).isEqualTo(2);
        assertThat(twoHelpers.maxHelpers()).isEqualTo(2);
        bed.publisher.clear();

        bed.setBookingRules.set(rules(AZMAN, 60, 480, 1, 5, null, 30, "REQUEST", "STANDARD"));

        assertThat(bed.publisher.single(ListingTermsChangedIE.class).maxHelpers()).isEqualTo(3);
    }

    @Test
    @Tag("OFR-10")
    @Tag("OFR-9")
    @DisplayName("A provider covering a wider area sets a 60-minute buffer, and switches to Firm")
    void theBufferAndTierArePublished() {
        bed.setBookingRules.set(rules(AZMAN, 60, 480, 1, null, null, 60, "REQUEST", "FIRM"));

        ListingTerms stored = bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow();
        assertThat(stored.buffer().duration()).isEqualTo(Duration.ofMinutes(60));
        assertThat(stored.tier()).isEqualTo(CancellationTier.FIRM);
        ListingTermsChangedIE event = bed.publisher.single(ListingTermsChangedIE.class);
        assertThat(event.jobBuffer()).isEqualTo(Duration.ofMinutes(60));
        assertThat(event.cancellationTier()).isEqualTo("FIRM");
        assertThat(event.minimumNotice()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    @Tag("OFR-6")
    @DisplayName("An Instant provider at 1 hour that switches to Request moves to 2 hours and is told")
    void switchingToRequestRaisesTheNoticeAndTellsTheProvider() {
        ListingTestBed instant = instantProviderAtOneHour();

        ProviderSetBookingRulesResponse response = instant.setBookingRules.set(
                rules(AZMAN, 60, 480, 1, null, null, 30, "REQUEST", "STANDARD"));

        assertThat(response).isEqualTo(new ProviderSetBookingRulesResponse(KEMAS_HOME, 120, true));
        ListingTerms stored = instant.listings.termsOf(KEMAS_HOME_ID).orElseThrow();
        assertThat(stored.policy()).isEqualTo(AcceptancePolicy.REQUEST);
        assertThat(stored.notice().duration()).isEqualTo(Duration.ofHours(2));
        ListingTermsChangedIE event = instant.publisher.single(ListingTermsChangedIE.class);
        assertThat(event.acceptancePolicy()).isEqualTo("REQUEST");
        assertThat(event.minimumNotice()).isEqualTo(Duration.ofHours(2));
        assertThat(event.noticeRaised()).isTrue();
        assertThat(instant.notifier.told()).containsExactly(new NoticeRaised(KEMAS_HOME, Duration.ofHours(2)));
    }

    @Test
    @Tag("OFR-6")
    @DisplayName("Switching to Request with 3 hours' notice given raises nothing, so the provider isn't told")
    void aNoticeGivenWithTheSwitchIsNotARaise() {
        ListingTestBed instant = instantProviderAtOneHour();

        ProviderSetBookingRulesResponse response = instant.setBookingRules.set(
                rules(AZMAN, 60, 480, 1, null, 180L, 30, "REQUEST", "STANDARD"));

        assertThat(response).isEqualTo(new ProviderSetBookingRulesResponse(KEMAS_HOME, 180, false));
        assertThat(instant.publisher.single(ListingTermsChangedIE.class).noticeRaised()).isFalse();
        assertThat(instant.notifier.told()).isEmpty();
    }

    @Test
    @Tag("OFR-6")
    @DisplayName("Kemas Home (Request) can't go below 2 hours; nothing changes")
    void aNoticeBelowTheFloorIsRefused() {
        assertThatThrownBy(() -> bed.setBookingRules.set(rules(AZMAN, 120, 360, 1, null, 60L, 30, "REQUEST", "FIRM")))
                .isInstanceOf(NoticeBelowFloorException.class);

        ListingTerms stored = bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow();
        assertThat(stored.tier()).isEqualTo(CancellationTier.STANDARD);
        assertThat(stored.bounds().minHours()).isEqualTo(Duration.ofHours(1));
        assertThat(bed.publisher.published()).isEmpty();
        assertThat(bed.notifier.told()).isEmpty();
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Bounds under Homa's floor of 1 hour in 15-minute steps are refused")
    void boundsUnderTheFloorAreRefused() {
        assertThatThrownBy(() -> bed.setBookingRules.set(rules(AZMAN, 45, 360, 1, null, null, 30, "REQUEST", "STANDARD")))
                .isInstanceOf(InvalidBookingBoundsException.class);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("Only Kemas Home's owner sets its booking rules")
    void onlyTheOwnerSetsBookingRules() {
        assertThatThrownBy(() -> bed.setBookingRules.set(rules(HANA, 120, 360, 1, null, null, 30, "INSTANT", "FIRM")))
                .isInstanceOf(NotAllowedException.class);

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow().policy()).isEqualTo(AcceptancePolicy.REQUEST);
        assertThat(bed.publisher.published()).isEmpty();
    }

    private static ListingTestBed instantProviderAtOneHour() {
        ListingTestBed instant = new ListingTestBed().withHomaPresets().withKemasHomeTeam();
        instant.createListing.create(kemasHomeCreation(Money.myr(25), "INSTANT"));
        instant.setBookingRules.set(rules(AZMAN, 60, 480, 1, null, 60L, 30, "INSTANT", "STANDARD"));
        instant.publisher.clear();
        return instant;
    }

    private static ProviderSetBookingRulesRequest rules(String actingPersonId, long minMinutes, long maxMinutes,
                                                        int minHelpers, Integer maxHelpers, Long noticeMinutes,
                                                        long bufferMinutes, String policy, String tier) {
        return new ProviderSetBookingRulesRequest(actingPersonId, KEMAS_HOME, minMinutes, maxMinutes, minHelpers,
                maxHelpers, noticeMinutes, bufferMinutes, policy, tier);
    }
}
