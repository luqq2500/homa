package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.RINA;
import static com.homa.provider.domain.ProviderFixtures.RINA_PERMIT_EXPIRY;
import static com.homa.provider.domain.ProviderFixtures.RINA_PERSON;
import static com.homa.provider.domain.ProviderFixtures.SITI;
import static com.homa.provider.domain.ProviderFixtures.SITI_PERSON;
import static com.homa.provider.domain.ProviderFixtures.TEAM_ADDED_AT;
import static com.homa.provider.domain.ProviderFixtures.TEAM_ADDED_ON;
import static com.homa.provider.domain.ProviderFixtures.VERIFIED_UNTIL;
import static com.homa.provider.domain.ProviderFixtures.openedKemasHome;
import static com.homa.provider.domain.ProviderFixtures.rina;
import static com.homa.provider.domain.ProviderFixtures.rinaPermit;
import static com.homa.provider.domain.ProviderFixtures.rinaVerified;
import static com.homa.provider.domain.ProviderFixtures.siti;
import static com.homa.provider.domain.ProviderFixtures.sitiVerified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.BusinessCalendar;
import com.homa.provider.domain.Eligibility.ForeignWorker;
import com.homa.provider.domain.Eligibility.Local;
import com.homa.provider.domain.exception.EmploymentNotConfirmedException;
import com.homa.provider.domain.exception.MemberUnderageException;
import com.homa.provider.domain.exception.ProviderRuleViolationException;
import com.homa.provider.domain.exception.WorkPermitExpiresTooSoonException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TeamMemberAddingTest {

    private static final TeamMemberId TRAINEE = new TeamMemberId("member-trainee");
    private static final TeamMemberId CANDIDATE = new TeamMemberId("member-candidate");
    private static final PersonId TRAINEE_PERSON = new PersonId("person-trainee");
    private static final PersonId CANDIDATE_PERSON = new PersonId("person-candidate");

    private final Provider kemasHome = openedKemasHome();

    private static VerifiedPersonFacts foreignCandidateWithDaysLeft(int days) {
        return VerifiedPersonFacts.foreign(CANDIDATE_PERSON, true, TEAM_ADDED_ON.plusDays(days), VERIFIED_UNTIL);
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("Kemas Home can't add a 17-year-old trainee.")
    void traineeUnder18CannotBeAdded() {
        VerifiedPersonFacts seventeen = VerifiedPersonFacts.local(TRAINEE_PERSON, false, VERIFIED_UNTIL);

        assertThatThrownBy(() -> kemasHome.addTeamMember(TRAINEE, TeamRole.HELPER, seventeen, false, Affiliation.none(), TEAM_ADDED_AT))
                .isInstanceOf(MemberUnderageException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(TRAINEE.value());
        assertThatThrownBy(() -> kemasHome.addTeamMember(TRAINEE, TeamRole.SUPERVISOR, seventeen, false,
                Affiliation.none(), TEAM_ADDED_AT))
                .isInstanceOf(MemberUnderageException.class);
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("A helper who turned 18 last week can be added.")
    void helperWhoPassedTheAgeCheckIsAdded() {
        VerifiedPersonFacts turned18LastWeek = VerifiedPersonFacts.local(CANDIDATE_PERSON, true, VERIFIED_UNTIL);

        TeamMember helper = kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, turned18LastWeek, false,
                Affiliation.none(), TEAM_ADDED_AT);

        assertThat(helper.isActive()).isTrue();
        assertThat(helper.personId()).isEqualTo(CANDIDATE_PERSON);
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("A foreign candidate must be aged 18 or above too, however long their permit runs.")
    void foreignCandidateUnder18CannotBeAdded() {
        VerifiedPersonFacts underage = VerifiedPersonFacts.foreign(CANDIDATE_PERSON, false, RINA_PERMIT_EXPIRY, VERIFIED_UNTIL);

        assertThatThrownBy(() -> kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, underage, true, Affiliation.none(), TEAM_ADDED_AT))
                .isInstanceOf(MemberUnderageException.class);
    }

    @Test
    @Tag("HLP-2")
    @DisplayName("Siti verifies with her MyKad.")
    void localVerifiedWithHerMyKadIsAddedAsALocalHelper() {
        TeamMember siti = siti(kemasHome);

        assertThat(siti.id()).isEqualTo(SITI);
        assertThat(siti.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(siti.personId()).isEqualTo(SITI_PERSON);
        assertThat(siti.role()).isEqualTo(TeamRole.HELPER);
        assertThat(siti.eligibility()).isEqualTo(new Local());
        assertThat(siti.eligibility().isLocal()).isTrue();
        assertThat(siti.addedAt()).isEqualTo(TEAM_ADDED_AT);
        assertThat(siti.isActive()).isTrue();
        assertThat(siti.isActiveHelper()).isTrue();
    }

    @Test
    @Tag("HLP-2")
    @DisplayName("A local member needs no work permit and no confirmation of lawful work.")
    void localMemberNeedsNoEmploymentConfirmation() {
        TeamMember withoutTick = kemasHome.addTeamMember(SITI, TeamRole.HELPER, sitiVerified(), false, Affiliation.none(), TEAM_ADDED_AT);
        TeamMember withTick = kemasHome.addTeamMember(SITI, TeamRole.HELPER, sitiVerified(), true, Affiliation.none(), TEAM_ADDED_AT);

        assertThat(withoutTick.eligibility()).isEqualTo(new Local());
        assertThat(withTick.eligibility()).isEqualTo(new Local());
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("Rina's permit has 200 days left, so she's added.")
    void foreignHelperWithAPermitExpiringIn200DaysIsAdded() {
        TeamMember rina = rina(kemasHome);

        assertThat(TEAM_ADDED_ON.plusDays(200)).isEqualTo(RINA_PERMIT_EXPIRY);
        assertThat(rina.id()).isEqualTo(RINA);
        assertThat(rina.personId()).isEqualTo(RINA_PERSON);
        assertThat(rina.eligibility()).isEqualTo(new ForeignWorker(RINA_PERMIT_EXPIRY, true));
        assertThat(rina.eligibility().isLocal()).isFalse();
        assertThat(rina.isActiveHelper()).isTrue();
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("A candidate with 60 days left waits until their permit is renewed.")
    void foreignCandidateWithAPermitExpiringIn60DaysCannotBeAdded() {
        assertThatThrownBy(() -> kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, foreignCandidateWithDaysLeft(60),
                true, Affiliation.none(), TEAM_ADDED_AT))
                .isInstanceOf(WorkPermitExpiresTooSoonException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(CANDIDATE.value());

        VerifiedPersonFacts renewed = foreignCandidateWithDaysLeft(60 + 365);
        assertThat(kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, renewed, true, Affiliation.none(), TEAM_ADDED_AT).isActive())
                .isTrue();
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("A permit with exactly 90 days left when the member is added is enough; 89 days is not.")
    void foreignMemberNeedsAtLeast90DaysOnTheirPermitWhenAdded() {
        assertThat(kemasHome.addTeamMember(CANDIDATE, TeamRole.HELPER, foreignCandidateWithDaysLeft(90), true,
                Affiliation.none(), TEAM_ADDED_AT).isActive()).isTrue();
        assertThatThrownBy(() -> kemasHome.addTeamMember(CANDIDATE, TeamRole.SUPERVISOR,
                foreignCandidateWithDaysLeft(89), true, Affiliation.none(), TEAM_ADDED_AT))
                .isInstanceOf(WorkPermitExpiresTooSoonException.class);
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("The days count from when the member is added: added 110 days later, Rina has 90 days left and is"
            + " added; a day later she has 89 and isn't.")
    void permitDaysCountFromWhenTheMemberIsAdded() {
        Instant ninetyDaysLeft = BusinessCalendar.plusDays(TEAM_ADDED_AT, 110);
        Instant eightyNineDaysLeft = BusinessCalendar.plusDays(TEAM_ADDED_AT, 111);

        assertThat(kemasHome.addTeamMember(RINA, TeamRole.HELPER, rinaVerified(), true, Affiliation.none(), ninetyDaysLeft).addedAt())
                .isEqualTo(ninetyDaysLeft);
        assertThatThrownBy(() -> kemasHome.addTeamMember(RINA, TeamRole.HELPER, rinaVerified(), true,
                Affiliation.none(), eightyNineDaysLeft))
                .isInstanceOf(WorkPermitExpiresTooSoonException.class);
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("A foreign worker's eligibility needs the permit's expiry date.")
    void foreignWorkerEligibilityNeedsThePermitExpiry() {
        assertThatThrownBy(() -> new ForeignWorker(null, true)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("HLP-4")
    @DisplayName("Kemas Home confirms Rina is employed by Kemas Home Sdn Bhd. Without it, Rina can't be added.")
    void foreignHelperCannotBeAddedWithoutTheProvidersConfirmation() {
        assertThatThrownBy(() -> kemasHome.addTeamMember(RINA, TeamRole.HELPER, rinaVerified(), false, Affiliation.none(), TEAM_ADDED_AT))
                .isInstanceOf(EmploymentNotConfirmedException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(RINA.value());

        TeamMember rina = rina(kemasHome);

        assertThat(rina.eligibility()).isEqualTo(rinaPermit());
        assertThat(((ForeignWorker) rina.eligibility()).employmentConfirmed()).isTrue();
    }
}
