package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.FARID;
import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.SITI;
import static com.homa.provider.domain.ProviderFixtures.TEAM_VERIFIED_AT;
import static com.homa.provider.domain.ProviderFixtures.farid;
import static com.homa.provider.domain.ProviderFixtures.helpers;
import static com.homa.provider.domain.ProviderFixtures.liveKemasHome;
import static com.homa.provider.domain.ProviderFixtures.registeredKemasHome;
import static com.homa.provider.domain.ProviderFixtures.siti;
import static com.homa.provider.domain.ProviderFixtures.storedMember;
import static com.homa.provider.domain.ProviderFixtures.verifiedKemasHome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.BusinessCalendar;
import com.homa.provider.domain.exception.NoActiveHelperException;
import com.homa.provider.domain.exception.ProviderAlreadyLiveException;
import com.homa.provider.domain.exception.ProviderNotVerifiedException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderGoLiveTest {

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home finishes its business verification but hasn't added a helper. It can't go live until Siti passes verification.")
    void providerCannotGoLiveWithoutAnActiveHelper() {
        Provider kemasHome = verifiedKemasHome();

        assertThatThrownBy(() -> kemasHome.goLive(new Team(List.of()).activeHelperCount()))
                .isInstanceOf(NoActiveHelperException.class)
                .hasMessageContaining(KEMAS_HOME.value());
        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.VERIFIED);

        kemasHome.goLive(new Team(List.of(siti(kemasHome))).activeHelperCount());

        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.LIVE);
        assertThat(kemasHome.isLive()).isTrue();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A supervisor is not a helper: Kemas Home can't go live with Farid alone.")
    void providerCannotGoLiveWithOnlyASupervisor() {
        Provider kemasHome = verifiedKemasHome();
        Team faridOnly = new Team(List.of(farid(kemasHome)));

        assertThatThrownBy(() -> kemasHome.goLive(faridOnly.activeHelperCount()))
                .isInstanceOf(NoActiveHelperException.class);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A deactivated or suspended helper is not active, so it doesn't let Kemas Home go live.")
    void providerCannotGoLiveWithOnlyInactiveHelpers() {
        Provider kemasHome = verifiedKemasHome();
        Team inactiveHelpers = new Team(List.of(
                storedMember(SITI, TeamRole.HELPER, true, false),
                storedMember(SITI, TeamRole.HELPER, false, true)));

        assertThatThrownBy(() -> kemasHome.goLive(inactiveHelpers.activeHelperCount()))
                .isInstanceOf(NoActiveHelperException.class);
    }

    @Test
    @Tag("PRV-3")
    @DisplayName("Kemas Home goes live with Siti, Nurul and Rina only. Farid is added as a supervisor a month later.")
    void providerGoesLiveWithoutASupervisorAndAddsOneLater() {
        Provider kemasHome = verifiedKemasHome();

        kemasHome.goLive(helpers(kemasHome).activeHelperCount());

        assertThat(kemasHome.isLive()).isTrue();

        Instant aMonthLater = BusinessCalendar.plusMonths(TEAM_VERIFIED_AT, 1);
        TeamMember farid = kemasHome.addTeamMember(FARID, TeamRole.SUPERVISOR, new Eligibility.Citizen(),
                new MemberVerification(VerificationOutcome.PASSED, ProviderFixtures.FARID_BORN, aMonthLater));

        assertThat(farid.role()).isEqualTo(TeamRole.SUPERVISOR);
        assertThat(farid.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(farid.isActive()).isTrue();
        assertThat(farid.verifiedAt()).isEqualTo(aMonthLater);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Kemas Home can't go live before its business is verified, even with helpers ready.")
    void providerCannotGoLiveBeforeItsBusinessIsVerified() {
        Provider kemasHome = registeredKemasHome();

        assertThatThrownBy(() -> kemasHome.goLive(3)).isInstanceOf(ProviderNotVerifiedException.class);
        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.REGISTERED);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A live provider goes live only once.")
    void providerGoesLiveOnlyOnce() {
        Provider kemasHome = liveKemasHome();

        assertThatThrownBy(() -> kemasHome.goLive(3))
                .isInstanceOf(ProviderAlreadyLiveException.class)
                .hasMessageContaining(KEMAS_HOME.value());
    }
}
