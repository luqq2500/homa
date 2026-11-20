package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.FARID;
import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.RINA;
import static com.homa.provider.domain.ProviderFixtures.RINA_BORN;
import static com.homa.provider.domain.ProviderFixtures.RINA_PERMIT_EXPIRY;
import static com.homa.provider.domain.ProviderFixtures.SITI;
import static com.homa.provider.domain.ProviderFixtures.SITI_BORN;
import static com.homa.provider.domain.ProviderFixtures.TEAM_VERIFIED_AT;
import static com.homa.provider.domain.ProviderFixtures.TEAM_VERIFIED_ON;
import static com.homa.provider.domain.ProviderFixtures.citizen;
import static com.homa.provider.domain.ProviderFixtures.failedVerification;
import static com.homa.provider.domain.ProviderFixtures.passedVerification;
import static com.homa.provider.domain.ProviderFixtures.rina;
import static com.homa.provider.domain.ProviderFixtures.siti;
import static com.homa.provider.domain.ProviderFixtures.verifiedKemasHome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.provider.domain.Eligibility.ForeignWorker;
import com.homa.provider.domain.exception.EmploymentNotConfirmedException;
import com.homa.provider.domain.exception.MemberNotVerifiedException;
import com.homa.provider.domain.exception.MemberUnderageException;
import com.homa.provider.domain.exception.WorkPermitExpiresTooSoonException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TeamMemberAddingTest {

    private static final TeamMemberId TRAINEE = new TeamMemberId("member-trainee");
    private static final TeamMemberId CANDIDATE = new TeamMemberId("member-candidate");

    private final Provider kemasHome = verifiedKemasHome();

    @Test
    @Tag("HLP-1")
    @DisplayName("Kemas Home tries to add a 17-year-old trainee. Verification fails on age, so the trainee can't be added.")
    void traineeUnder18CannotBeAdded() {
        LocalDate seventeenYearsOld = TEAM_VERIFIED_ON.minusYears(17).minusMonths(8);

        assertThatThrownBy(() -> kemasHome.addTeamMember(TRAINEE, TeamRole.HELPER, citizen(),
                passedVerification(seventeenYearsOld)))
                .isInstanceOf(MemberUnderageException.class)
                .hasMessageContaining(TRAINEE.value());
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("A new helper who turned 18 last week passes.")
    void helperWhoTurned18LastWeekIsAdded() {
        LocalDate turned18LastWeek = TEAM_VERIFIED_ON.minusYears(18).minusWeeks(1);

        TeamMember helper = kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, citizen(),
                passedVerification(turned18LastWeek));

        assertThat(helper.isActive()).isTrue();
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("A supervisor must be aged 18 or above too.")
    void supervisorUnder18CannotBeAdded() {
        LocalDate eighteenTomorrow = TEAM_VERIFIED_ON.minusYears(18).plusDays(1);

        assertThatThrownBy(() -> kemasHome.addTeamMember(FARID, TeamRole.SUPERVISOR, citizen(),
                passedVerification(eighteenTomorrow)))
                .isInstanceOf(MemberUnderageException.class);
    }

    @Test
    @Tag("HLP-2")
    @DisplayName("Siti verifies with her identity card and is added as a citizen helper.")
    void citizenVerifiedWithHerIdentityCardIsAddedAsACitizenHelper() {
        TeamMember siti = siti(kemasHome);

        assertThat(siti.id()).isEqualTo(SITI);
        assertThat(siti.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(siti.role()).isEqualTo(TeamRole.HELPER);
        assertThat(siti.eligibility()).isEqualTo(new Eligibility.Citizen());
        assertThat(siti.eligibility().isCitizen()).isTrue();
        assertThat(siti.verifiedAt()).isEqualTo(TEAM_VERIFIED_AT);
        assertThat(siti.isActive()).isTrue();
        assertThat(siti.isActiveHelper()).isTrue();
    }

    @Test
    @Tag("HLP-2")
    @DisplayName("A citizen whose identity card isn't verified can't be added.")
    void citizenNotVerifiedAsACitizenCannotBeAdded() {
        assertThatThrownBy(() -> kemasHome.addTeamMember(SITI, TeamRole.HELPER, citizen(),
                failedVerification(SITI_BORN)))
                .isInstanceOf(MemberNotVerifiedException.class)
                .hasMessageContaining(SITI.value());
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("Rina's permit expires in 200 days, so she passes.")
    void foreignHelperWithAPermitExpiringIn200DaysIsAdded() {
        TeamMember rina = rina(kemasHome);

        assertThat(rina.id()).isEqualTo(RINA);
        assertThat(rina.eligibility()).isEqualTo(new ForeignWorker(RINA_PERMIT_EXPIRY, true));
        assertThat(rina.eligibility().isCitizen()).isFalse();
        assertThat(rina.isActiveHelper()).isTrue();
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("Another candidate's permit expires in 60 days, so they can't be added until their permit is renewed.")
    void foreignCandidateWithAPermitExpiringIn60DaysCannotBeAdded() {
        ForeignWorker sixtyDaysLeft = new ForeignWorker(TEAM_VERIFIED_ON.plusDays(60), true);

        assertThatThrownBy(() -> kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, sixtyDaysLeft,
                passedVerification(RINA_BORN)))
                .isInstanceOf(WorkPermitExpiresTooSoonException.class)
                .hasMessageContaining(CANDIDATE.value());

        ForeignWorker renewed = new ForeignWorker(TEAM_VERIFIED_ON.plusDays(60).plusYears(1), true);
        assertThat(kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, renewed, passedVerification(RINA_BORN))
                .isActive()).isTrue();
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("A permit expiring exactly 90 days after verification is enough; 89 days is not.")
    void foreignHelperNeedsAtLeast90DaysOnTheirPermit() {
        ForeignWorker ninetyDaysLeft = new ForeignWorker(TEAM_VERIFIED_ON.plusDays(90), true);
        ForeignWorker eightyNineDaysLeft = new ForeignWorker(TEAM_VERIFIED_ON.plusDays(89), true);

        assertThat(kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, ninetyDaysLeft, passedVerification(RINA_BORN))
                .isActive()).isTrue();
        assertThatThrownBy(() -> kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, eightyNineDaysLeft,
                passedVerification(RINA_BORN)))
                .isInstanceOf(WorkPermitExpiresTooSoonException.class);
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("A foreign candidate not verified as legally allowed to work can't be added.")
    void foreignCandidateNotVerifiedAsAllowedToWorkCannotBeAdded() {
        assertThatThrownBy(() -> kemasHome.addTeamMember(RINA, TeamRole.HELPER, ProviderFixtures.rinaPermit(),
                failedVerification(RINA_BORN)))
                .isInstanceOf(MemberNotVerifiedException.class);
    }

    @Test
    @Tag("HLP-4")
    @DisplayName("When adding Rina, Kemas Home ticks the confirmation that Rina is lawfully employed by Kemas Home. Without the tick, Rina can't be added.")
    void foreignHelperCannotBeAddedWithoutTheProvidersEmploymentConfirmation() {
        ForeignWorker unconfirmed = new ForeignWorker(RINA_PERMIT_EXPIRY, false);

        assertThatThrownBy(() -> kemasHome.addTeamMember(RINA, TeamRole.HELPER, unconfirmed,
                passedVerification(RINA_BORN)))
                .isInstanceOf(EmploymentNotConfirmedException.class)
                .hasMessageContaining(RINA.value());

        TeamMember rina = kemasHome.addTeamMember(RINA, TeamRole.HELPER, ProviderFixtures.rinaPermit(),
                passedVerification(RINA_BORN));
        assertThat(((ForeignWorker) rina.eligibility()).employmentConfirmed()).isTrue();
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("A foreign worker's eligibility needs the permit's expiry date.")
    void foreignWorkerEligibilityNeedsThePermitExpiry() {
        assertThatThrownBy(() -> new ForeignWorker(null, true)).isInstanceOf(NullPointerException.class);
    }
}
