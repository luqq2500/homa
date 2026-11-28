package com.homa.provider.domain;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.provider.domain.ProviderFixtures.FARID;
import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.NURUL;
import static com.homa.provider.domain.ProviderFixtures.RINA;
import static com.homa.provider.domain.ProviderFixtures.SITI;
import static com.homa.provider.domain.ProviderFixtures.VERIFIED_UNTIL;
import static com.homa.provider.domain.ProviderFixtures.farid;
import static com.homa.provider.domain.ProviderFixtures.openedKemasHome;
import static com.homa.provider.domain.ProviderFixtures.siti;
import static com.homa.provider.domain.ProviderFixtures.storedMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.provider.domain.exception.MemberAlreadyDeactivatedException;
import com.homa.provider.domain.exception.MemberNotDeactivatedException;
import com.homa.provider.domain.exception.MemberRemovedException;
import com.homa.provider.domain.exception.ProviderRuleViolationException;
import com.homa.provider.domain.exception.VerificationLapsedException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TeamMemberDeactivationTest {

    /** Siti's two weeks' leave starts on Monday 14 September 2026 and she is back on Monday 28 September. */
    private static final Instant BACK_FROM_LEAVE = at(2026, 9, 28, 8, 0);

    @Test
    @Tag("HLP-5")
    @DisplayName("Siti is deactivated for two weeks' leave and reactivated when she's back.")
    void sitiIsDeactivatedForLeaveAndReactivatedWhenBack() {
        TeamMember siti = siti(openedKemasHome());

        siti.deactivate();

        assertThat(siti.isDeactivated()).isTrue();
        assertThat(siti.isActive()).isFalse();
        assertThat(siti.isRemoved()).isFalse();
        assertThat(siti.isSuspended()).isFalse();

        siti.reactivate(BACK_FROM_LEAVE);

        assertThat(siti.isDeactivated()).isFalse();
        assertThat(siti.isActive()).isTrue();
        assertThat(siti.isActiveHelper()).isTrue();
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("Siti is already deactivated, so deactivating her again is refused; Nurul isn't, so reactivating her is.")
    void deactivationAndReactivationNeedTheRightState() {
        TeamMember siti = storedMember(SITI, TeamRole.HELPER, true, false);
        TeamMember nurul = storedMember(NURUL, TeamRole.HELPER, false, false);

        assertThatThrownBy(siti::deactivate)
                .isInstanceOf(MemberAlreadyDeactivatedException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(SITI.value());
        assertThatThrownBy(() -> nurul.reactivate(BACK_FROM_LEAVE))
                .isInstanceOf(MemberNotDeactivatedException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(NURUL.value());
        assertThat(siti.isDeactivated()).isTrue();
        assertThat(nurul.isDeactivated()).isFalse();
    }

    @Test
    @Tag("HLP-6")
    @DisplayName("Nurul, deactivated on Thursday, isn't active, so she can't be rostered on Saturday and isn't counted as a helper.")
    void deactivatedMemberIsNotActive() {
        Provider kemasHome = openedKemasHome();
        TeamMember siti = siti(kemasHome);
        TeamMember nurul = ProviderFixtures.nurul(kemasHome);
        TeamMember farid = farid(kemasHome);

        nurul.deactivate();
        farid.deactivate();

        assertThat(nurul.isActive()).isFalse();
        assertThat(nurul.isActiveHelper()).isFalse();
        assertThat(farid.isActive()).isFalse();
        assertThat(siti.isActive()).isTrue();
        assertThat(new Team(List.of(siti, nurul, farid)).activeHelperCount()).isEqualTo(1);
    }

    @Test
    @Tag("HLP-5")
    @Tag("HLP-10")
    @DisplayName("A deactivated member stays on the team: they still belong to the provider.")
    void deactivatedMemberStaysOnTheTeam() {
        TeamMember siti = siti(openedKemasHome());

        siti.deactivate();

        assertThat(siti.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(siti.isRemoved()).isFalse();
    }

    @Test
    @Tag("HLP-7")
    @DisplayName("Farid's re-verification fell due while he was deactivated, so he can't be reactivated until he re-verifies.")
    void memberWhoseVerificationFellDueCantBeReactivated() {
        TeamMember farid = farid(openedKemasHome());
        farid.deactivate();
        Instant afterItFellDue = VERIFIED_UNTIL.plusSeconds(86_400);

        assertThatThrownBy(() -> farid.reactivate(afterItFellDue))
                .isInstanceOf(VerificationLapsedException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(FARID.value())
                .hasMessageContaining(VERIFIED_UNTIL.toString());
        assertThat(farid.isDeactivated()).isTrue();
        assertThat(farid.isActive()).isFalse();
    }

    @Test
    @Tag("HLP-7")
    @DisplayName("Farid re-verifies, his next due date moves six months on, and Kemas Home can reactivate him.")
    void memberWhoReVerifiedCanBeReactivated() {
        TeamMember farid = farid(openedKemasHome());
        farid.deactivate();
        Instant afterItFellDue = VERIFIED_UNTIL.plusSeconds(86_400);
        Instant nextDue = at(2027, 8, 25, 9, 0);

        farid.recordVerification(nextDue);
        farid.reactivate(afterItFellDue);

        assertThat(farid.verifiedUntil()).isEqualTo(nextDue);
        assertThat(farid.isDeactivated()).isFalse();
        assertThat(farid.isActive()).isTrue();
    }

    @Test
    @Tag("HLP-7")
    @DisplayName("A verification is valid up to the moment it falls due: one second before is valid, the moment itself isn't.")
    void verificationIsValidUntilItFallsDue() {
        TeamMember justBefore = storedMember(SITI, TeamRole.HELPER, true, false);
        TeamMember atTheMoment = storedMember(NURUL, TeamRole.HELPER, true, false);

        justBefore.reactivate(VERIFIED_UNTIL.minusSeconds(1));

        assertThat(justBefore.isActive()).isTrue();
        assertThatThrownBy(() -> atTheMoment.reactivate(VERIFIED_UNTIL))
                .isInstanceOf(VerificationLapsedException.class);
        assertThat(atTheMoment.isDeactivated()).isTrue();
    }

    @Test
    @Tag("HLP-7")
    @DisplayName("A member keeps the due date of their first verification, from when they were added.")
    void addedMemberKeepsTheirVerificationDueDate() {
        TeamMember siti = siti(openedKemasHome());

        assertThat(siti.verifiedUntil()).isEqualTo(VERIFIED_UNTIL);
    }

    @Test
    @Tag("HLP-7")
    @DisplayName("A due date is always known: a pass without one is refused.")
    void verificationNeedsADueDate() {
        TeamMember siti = siti(openedKemasHome());

        assertThatThrownBy(() -> siti.recordVerification(null)).isInstanceOf(NullPointerException.class);
        assertThat(siti.verifiedUntil()).isEqualTo(VERIFIED_UNTIL);
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("Siti, deactivated and also suspended, is reactivated by the provider but stays inactive until she is no longer suspended.")
    void reactivatingASuspendedMemberLeavesThemInactive() {
        TeamMember siti = storedMember(SITI, TeamRole.HELPER, true, true);

        siti.reactivate(BACK_FROM_LEAVE);

        assertThat(siti.isDeactivated()).isFalse();
        assertThat(siti.isSuspended()).isTrue();
        assertThat(siti.isActive()).isFalse();
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("A removed member can't be deactivated or reactivated: they can only come back as a new member.")
    void removedMemberCantBeDeactivatedOrReactivated() {
        TeamMember removedAndDeactivated = storedMember(RINA, TeamRole.HELPER, true, false, true);
        TeamMember removed = storedMember(RINA, TeamRole.HELPER, false, false, true);

        assertThatThrownBy(() -> removedAndDeactivated.reactivate(BACK_FROM_LEAVE))
                .isInstanceOf(MemberRemovedException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(RINA.value());
        assertThatThrownBy(removed::deactivate).isInstanceOf(MemberRemovedException.class);
        assertThatThrownBy(() -> removed.reactivate(BACK_FROM_LEAVE)).isInstanceOf(MemberRemovedException.class);
        assertThat(removedAndDeactivated.isDeactivated()).isTrue();
        assertThat(removed.isDeactivated()).isFalse();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("When all Kemas Home's helpers are deactivated for a holiday week, it disappears from search; when one is back, it is bookable.")
    void providerDisappearsFromSearchWhenAllItsHelpersAreDeactivated() {
        Provider kemasHome = ProviderFixtures.liveKemasHome();
        TeamMember siti = siti(kemasHome);
        TeamMember nurul = ProviderFixtures.nurul(kemasHome);
        TeamMember rina = ProviderFixtures.rina(kemasHome);

        siti.deactivate();
        nurul.deactivate();
        rina.deactivate();

        assertThat(kemasHome.bookabilityFor(new Team(List.of(siti, nurul, rina)).activeHelperCount()))
                .isEqualTo(Bookability.NO_ACTIVE_HELPERS);

        siti.reactivate(BACK_FROM_LEAVE);

        assertThat(kemasHome.bookabilityFor(new Team(List.of(siti, nurul, rina)).activeHelperCount()))
                .isEqualTo(Bookability.BOOKABLE);
    }
}
