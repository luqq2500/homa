package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderStandingTest {

    @Test
    @Tag("PRV-2")
    @DisplayName("A provider Listing has heard nothing about isn't live, isn't bookable and has no active helpers")
    void anUnknownProviderIsNotListed() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);

        assertThat(kemasHome.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(kemasHome.live()).isFalse();
        assertThat(kemasHome.bookable()).isFalse();
        assertThat(kemasHome.isListed()).isFalse();
        assertThat(kemasHome.activeHelperCount()).isZero();
        assertThat(kemasHome.activeHelperIds()).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home goes live and shows in search")
    void aProviderThatWentLiveIsListed() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);

        kemasHome.wentLive();

        assertThat(kemasHome.live()).isTrue();
        assertThat(kemasHome.bookable()).isTrue();
        assertThat(kemasHome.isListed()).isTrue();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("When all its helpers are deactivated for a holiday week, Kemas Home disappears from search, and "
            + "returns when it is bookable again")
    void aLiveProviderThatIsNotBookableIsNotListed() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);
        kemasHome.wentLive();

        kemasHome.changeBookability(false);

        assertThat(kemasHome.live()).isTrue();
        assertThat(kemasHome.bookable()).isFalse();
        assertThat(kemasHome.isListed()).isFalse();

        kemasHome.changeBookability(true);

        assertThat(kemasHome.isListed()).isTrue();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A provider that is bookable but not yet live doesn't show")
    void aBookableProviderThatIsNotLiveIsNotListed() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);

        kemasHome.changeBookability(true);

        assertThat(kemasHome.bookable()).isTrue();
        assertThat(kemasHome.isListed()).isFalse();
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Siti, Nurul and Rina are active helpers, so Kemas Home has 3; supervisor Farid doesn't count")
    void onlyActiveHelpersCount() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);

        assertThat(kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER)).isTrue();
        assertThat(kemasHome.memberActivated(NURUL_MEMBER_ID, MemberRole.HELPER)).isTrue();
        assertThat(kemasHome.memberActivated(RINA_MEMBER_ID, MemberRole.HELPER)).isTrue();
        assertThat(kemasHome.memberActivated(FARID_MEMBER_ID, MemberRole.SUPERVISOR)).isFalse();

        assertThat(kemasHome.activeHelperCount()).isEqualTo(3);
        assertThat(kemasHome.activeHelperIds()).containsExactlyInAnyOrder(SITI_MEMBER_ID, NURUL_MEMBER_ID, RINA_MEMBER_ID);
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Siti activated twice is still one active helper")
    void aHelperActivatedTwiceCountsOnce() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);
        kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER);

        assertThat(kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER)).isFalse();
        assertThat(kemasHome.activeHelperCount()).isEqualTo(1);
    }

    @Test
    @Tag("HLP-5")
    @Tag("OFR-5")
    @DisplayName("Siti is deactivated for two weeks' leave and reactivated when she's back: she stops counting, then "
            + "counts again")
    void aDeactivatedHelperStopsCountingUntilReactivated() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);
        kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER);
        kemasHome.memberActivated(NURUL_MEMBER_ID, MemberRole.HELPER);

        assertThat(kemasHome.memberDeactivated(SITI_MEMBER_ID)).isTrue();

        assertThat(kemasHome.activeHelperCount()).isEqualTo(1);
        assertThat(kemasHome.activeHelperIds()).containsExactly(NURUL_MEMBER_ID);

        assertThat(kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER)).isTrue();

        assertThat(kemasHome.activeHelperCount()).isEqualTo(2);
        assertThat(kemasHome.activeHelperIds()).containsExactlyInAnyOrder(SITI_MEMBER_ID, NURUL_MEMBER_ID);
    }

    @Test
    @Tag("HLP-8")
    @Tag("OFR-5")
    @DisplayName("Rina resigns and is removed the same day: she stops counting as an active helper")
    void aRemovedHelperStopsCounting() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);
        kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER);
        kemasHome.memberActivated(RINA_MEMBER_ID, MemberRole.HELPER);

        assertThat(kemasHome.memberDeactivated(RINA_MEMBER_ID)).isTrue();

        assertThat(kemasHome.activeHelperCount()).isEqualTo(1);
        assertThat(kemasHome.activeHelperIds()).containsExactly(SITI_MEMBER_ID);
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Supervisor Farid doesn't count, so deactivating him changes nothing customers book")
    void deactivatingASupervisorChangesNothing() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);
        kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER);
        kemasHome.memberActivated(FARID_MEMBER_ID, MemberRole.SUPERVISOR);

        assertThat(kemasHome.memberDeactivated(FARID_MEMBER_ID)).isFalse();

        assertThat(kemasHome.activeHelperCount()).isEqualTo(1);
        assertThat(kemasHome.activeHelperIds()).containsExactly(SITI_MEMBER_ID);
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("A member Listing never counted, such as Nurul before her activation arrives, changes nothing")
    void deactivatingAMemberThatWasNeverCountedChangesNothing() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);
        kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER);

        assertThat(kemasHome.memberDeactivated(NURUL_MEMBER_ID)).isFalse();

        assertThat(kemasHome.activeHelperIds()).containsExactly(SITI_MEMBER_ID);
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Siti deactivated twice, or the only helper deactivated, never takes the count below zero")
    void theCountNeverGoesBelowZero() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);
        kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER);
        assertThat(kemasHome.memberDeactivated(SITI_MEMBER_ID)).isTrue();

        assertThat(kemasHome.memberDeactivated(SITI_MEMBER_ID)).isFalse();
        assertThat(kemasHome.memberDeactivated(NURUL_MEMBER_ID)).isFalse();

        assertThat(kemasHome.activeHelperCount()).isZero();
        assertThat(kemasHome.activeHelperIds()).isEmpty();
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("Siti activated again after her leave is counted once, however often Provider says so")
    void aReactivatedHelperCountsOnce() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);
        kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER);
        kemasHome.memberDeactivated(SITI_MEMBER_ID);

        assertThat(kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER)).isTrue();
        assertThat(kemasHome.memberActivated(SITI_MEMBER_ID, MemberRole.HELPER)).isFalse();

        assertThat(kemasHome.activeHelperCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("A stored standing comes back as it was, and keeps counting from there")
    void aStoredStandingIsRebuilt() {
        ProviderStanding kemasHome = ProviderStanding.rebuild(KEMAS_HOME, true, false, Set.of(SITI_MEMBER_ID));

        assertThat(kemasHome.live()).isTrue();
        assertThat(kemasHome.bookable()).isFalse();
        assertThat(kemasHome.activeHelperIds()).containsExactly(SITI_MEMBER_ID);
        assertThat(kemasHome.memberActivated(NURUL_MEMBER_ID, MemberRole.HELPER)).isTrue();
        assertThat(kemasHome.activeHelperCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("A standing needs a provider, and a member needs an id")
    void missingIdsAreRefused() {
        ProviderStanding kemasHome = ProviderStanding.unknown(KEMAS_HOME);

        assertThatThrownBy(() -> ProviderStanding.unknown(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> kemasHome.memberActivated(null, MemberRole.HELPER))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> kemasHome.memberDeactivated(null)).isInstanceOf(NullPointerException.class);
    }
}
