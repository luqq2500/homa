package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.FARID;
import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.SITI;
import static com.homa.provider.domain.ProviderFixtures.TEAM_ADDED_AT;
import static com.homa.provider.domain.ProviderFixtures.farid;
import static com.homa.provider.domain.ProviderFixtures.faridVerified;
import static com.homa.provider.domain.ProviderFixtures.helpers;
import static com.homa.provider.domain.ProviderFixtures.kemasHomeWithVerifiedPayoutAccount;
import static com.homa.provider.domain.ProviderFixtures.liveKemasHome;
import static com.homa.provider.domain.ProviderFixtures.openedHanaCleaning;
import static com.homa.provider.domain.ProviderFixtures.openedKemasHome;
import static com.homa.provider.domain.ProviderFixtures.siti;
import static com.homa.provider.domain.ProviderFixtures.storedMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.BusinessCalendar;
import com.homa.provider.domain.exception.NoActiveHelperException;
import com.homa.provider.domain.exception.NoVerifiedPayoutAccountException;
import com.homa.provider.domain.exception.ProviderAlreadyLiveException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderGoLiveTest {

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home can't go live until its payout account is verified.")
    void providerCannotGoLiveUntilItsPayoutAccountIsVerified() {
        Provider kemasHome = openedKemasHome();
        int activeHelpers = helpers(kemasHome).activeHelperCount();

        assertThatThrownBy(() -> kemasHome.goLive(activeHelpers))
                .isInstanceOf(NoVerifiedPayoutAccountException.class)
                .hasMessageContaining(KEMAS_HOME.value());
        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.OPENED);

        kemasHome.recordPayoutAccountVerified(PayoutAccountHolder.BUSINESS);
        kemasHome.goLive(activeHelpers);

        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.LIVE);
        assertThat(kemasHome.isLive()).isTrue();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Hana Cleaning's payout account is in Hana's own name. Once it's verified, Hana Cleaning goes live with Hana's helper.")
    void payoutAccountInTheOwnersNameLetsAProviderGoLive() {
        Provider hanaCleaning = openedHanaCleaning();

        hanaCleaning.recordPayoutAccountVerified(PayoutAccountHolder.OWNER);
        hanaCleaning.goLive(1);

        assertThat(hanaCleaning.isLive()).isTrue();
        assertThat(hanaCleaning.verifiedPayoutAccount()).contains(PayoutAccountHolder.OWNER);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A payout account that is no longer verified doesn't let a provider go live.")
    void payoutAccountNoLongerVerifiedStopsAProviderGoingLive() {
        Provider kemasHome = kemasHomeWithVerifiedPayoutAccount();

        kemasHome.recordPayoutAccountNotVerified();

        assertThat(kemasHome.verifiedPayoutAccount()).isEmpty();
        assertThatThrownBy(() -> kemasHome.goLive(3)).isInstanceOf(NoVerifiedPayoutAccountException.class);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home's payout account is verified but it hasn't added a helper. It can't go live until Siti joins.")
    void providerCannotGoLiveWithoutAnActiveHelper() {
        Provider kemasHome = kemasHomeWithVerifiedPayoutAccount();

        assertThatThrownBy(() -> kemasHome.goLive(new Team(List.of()).activeHelperCount()))
                .isInstanceOf(NoActiveHelperException.class)
                .hasMessageContaining(KEMAS_HOME.value());
        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.OPENED);

        kemasHome.goLive(new Team(List.of(siti(kemasHome))).activeHelperCount());

        assertThat(kemasHome.isLive()).isTrue();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A supervisor is not a helper: Kemas Home can't go live with Farid alone.")
    void providerCannotGoLiveWithOnlyASupervisor() {
        Provider kemasHome = kemasHomeWithVerifiedPayoutAccount();
        Team faridOnly = new Team(List.of(farid(kemasHome)));

        assertThatThrownBy(() -> kemasHome.goLive(faridOnly.activeHelperCount()))
                .isInstanceOf(NoActiveHelperException.class);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A deactivated or suspended helper is not active, so it doesn't let Kemas Home go live.")
    void providerCannotGoLiveWithOnlyInactiveHelpers() {
        Provider kemasHome = kemasHomeWithVerifiedPayoutAccount();
        Team inactiveHelpers = new Team(List.of(
                storedMember(SITI, TeamRole.HELPER, true, false),
                storedMember(SITI, TeamRole.HELPER, false, true)));

        assertThatThrownBy(() -> kemasHome.goLive(inactiveHelpers.activeHelperCount()))
                .isInstanceOf(NoActiveHelperException.class);
    }

    @Test
    @Tag("PRV-3")
    @DisplayName("Kemas Home goes live with Siti, Nurul and Rina, and adds Farid as a supervisor a month later.")
    void providerGoesLiveWithoutASupervisorAndAddsOneLater() {
        Provider kemasHome = kemasHomeWithVerifiedPayoutAccount();

        kemasHome.goLive(helpers(kemasHome).activeHelperCount());

        assertThat(kemasHome.isLive()).isTrue();

        Instant aMonthLater = BusinessCalendar.plusMonths(TEAM_ADDED_AT, 1);
        TeamMember farid = kemasHome.addTeamMember(FARID, TeamRole.SUPERVISOR, faridVerified(), false, Affiliation.none(), aMonthLater);

        assertThat(farid.role()).isEqualTo(TeamRole.SUPERVISOR);
        assertThat(farid.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(farid.isActive()).isTrue();
        assertThat(farid.addedAt()).isEqualTo(aMonthLater);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A live provider goes live only once.")
    void providerGoesLiveOnlyOnce() {
        Provider kemasHome = liveKemasHome();

        assertThatThrownBy(() -> kemasHome.goLive(3))
                .isInstanceOf(ProviderAlreadyLiveException.class)
                .hasMessageContaining(KEMAS_HOME.value());

        kemasHome.recordPayoutAccountNotVerified();

        assertThatThrownBy(() -> kemasHome.goLive(0)).isInstanceOf(ProviderAlreadyLiveException.class);
    }
}
