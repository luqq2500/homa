package com.homa.provider.domain;

import static com.homa.fixtures.HomaFixtures.CHECK_IN_PERIOD;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.provider.domain.ProviderFixtures.RINA;
import static com.homa.provider.domain.ProviderFixtures.RINA_PERSON;
import static com.homa.provider.domain.ProviderFixtures.SITI;
import static com.homa.provider.domain.ProviderFixtures.VERIFIED_UNTIL;
import static com.homa.provider.domain.ProviderFixtures.openedKemasHome;
import static com.homa.provider.domain.ProviderFixtures.rina;
import static com.homa.provider.domain.ProviderFixtures.standardJobWindow;
import static com.homa.provider.domain.ProviderFixtures.storedMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.TimeInterval;
import com.homa.provider.domain.exception.MemberInRosteredWindowException;
import com.homa.provider.domain.exception.MemberRemovedException;
import com.homa.provider.domain.exception.ProviderRuleViolationException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TeamMemberRemovalTest {

    /** Rina resigns on Wednesday 2 December 2026, three days before Saturday's job. */
    private static final Instant WEDNESDAY = at(2026, 12, 2, 15, 0);
    private static final Instant CHECK_IN_START = CHECK_IN_PERIOD.start();

    private final Provider kemasHome = openedKemasHome();

    @Test
    @Tag("HLP-8")
    @DisplayName("Rina resigns and is removed the same day: she's no longer active.")
    void memberWhoResignsIsRemoved() {
        TeamMember rina = rina(kemasHome);

        rina.remove(RosteredWindows.none(), WEDNESDAY);

        assertThat(rina.isRemoved()).isTrue();
        assertThat(rina.isActive()).isFalse();
        assertThat(rina.isActiveHelper()).isFalse();
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("A deactivated member who no longer works for Kemas Home can be removed too.")
    void deactivatedMemberCanBeRemoved() {
        TeamMember rina = rina(kemasHome);
        rina.deactivate();

        rina.remove(RosteredWindows.none(), WEDNESDAY);

        assertThat(rina.isRemoved()).isTrue();
        assertThat(rina.isActive()).isFalse();
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("Rina was removed, so she can't be removed again, deactivated or reactivated: removal is final.")
    void removalIsFinal() {
        TeamMember rina = rina(kemasHome);
        rina.remove(RosteredWindows.none(), WEDNESDAY);

        assertThatThrownBy(() -> rina.remove(RosteredWindows.none(), WEDNESDAY))
                .isInstanceOf(MemberRemovedException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(RINA.value());
        assertThatThrownBy(rina::deactivate).isInstanceOf(MemberRemovedException.class);
        assertThatThrownBy(() -> rina.reactivate(WEDNESDAY)).isInstanceOf(MemberRemovedException.class);
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("If Rina returns a year later, she's added and verified as new: a new member, while the removed one stays removed.")
    void removedMemberReturnsOnlyAsANewMember() {
        TeamMember removedRina = rina(kemasHome);
        removedRina.remove(RosteredWindows.none(), WEDNESDAY);
        Instant aYearLater = at(2027, 12, 2, 10, 0);
        VerifiedPersonFacts renewedPermit = VerifiedPersonFacts.foreign(
                RINA_PERSON, true, LocalDate.of(2028, 10, 1), at(2028, 6, 2, 9, 0));
        TeamMemberId newMemberId = new TeamMemberId("member-rina-again");

        TeamMember newRina = kemasHome.addTeamMember(newMemberId, TeamRole.HELPER, renewedPermit, true,
                Affiliation.none(), aYearLater);

        assertThat(newRina.id()).isNotEqualTo(removedRina.id());
        assertThat(newRina.personId()).isEqualTo(removedRina.personId());
        assertThat(newRina.isActive()).isTrue();
        assertThat(newRina.isRemoved()).isFalse();
        assertThat(removedRina.isRemoved()).isTrue();
        assertThat(removedRina.isActive()).isFalse();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Rina quits at 11:00 during a job: Kemas Home can't remove her then.")
    void memberCantBeRemovedDuringTheJob() {
        TeamMember rina = rina(kemasHome);

        assertThatThrownBy(() -> rina.remove(standardJobWindow(), at(2026, 12, 5, 11, 0)))
                .isInstanceOf(MemberInRosteredWindowException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(RINA.value());

        assertThat(rina.isRemoved()).isFalse();
        assertThat(rina.isActive()).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Kemas Home removes Rina at 14:00, when the job has ended.")
    void memberCanBeRemovedWhenTheJobEnds() {
        TeamMember rina = rina(kemasHome);

        rina.remove(standardJobWindow(), JOB_END);

        assertThat(rina.isRemoved()).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("One minute before the job ends, Rina is still inside it and can't be removed.")
    void memberCantBeRemovedOneMinuteBeforeTheJobEnds() {
        TeamMember rina = rina(kemasHome);

        assertThatThrownBy(() -> rina.remove(standardJobWindow(), JOB_END.minus(Duration.ofMinutes(1))))
                .isInstanceOf(MemberInRosteredWindowException.class);
        assertThat(rina.isRemoved()).isFalse();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("At 9:45, when the Check-in Period starts, Rina is in the window and can't be removed.")
    void memberCantBeRemovedWhenTheCheckInPeriodStarts() {
        TeamMember rina = rina(kemasHome);

        assertThatThrownBy(() -> rina.remove(standardJobWindow(), CHECK_IN_START))
                .isInstanceOf(MemberInRosteredWindowException.class);
        assertThat(rina.isRemoved()).isFalse();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("One minute before the Check-in Period starts, Rina isn't in the window yet, so she can be removed.")
    void memberCanBeRemovedOneMinuteBeforeTheCheckInPeriod() {
        TeamMember rina = rina(kemasHome);

        rina.remove(standardJobWindow(), CHECK_IN_START.minus(Duration.ofMinutes(1)));

        assertThat(rina.isRemoved()).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @Tag("ROS-5")
    @DisplayName("Had Rina resigned on Wednesday, she could be removed then and replaced on Saturday's roster.")
    void memberRosteredOnALaterJobCanBeRemovedBeforeItsCheckInPeriod() {
        TeamMember rina = rina(kemasHome);

        rina.remove(standardJobWindow(), WEDNESDAY);

        assertThat(rina.isRemoved()).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A member on two rosters is refused inside either job, and removed between them.")
    void memberWithTwoWindowsIsRefusedInsideEither() {
        TimeInterval morning = new TimeInterval(at(2026, 12, 5, 7, 45), at(2026, 12, 5, 9, 0));
        TimeInterval afternoon = new TimeInterval(CHECK_IN_START, JOB_END);
        RosteredWindows both = RosteredWindows.of(List.of(morning, afternoon));
        TeamMember rina = rina(kemasHome);

        assertThatThrownBy(() -> rina.remove(both, at(2026, 12, 5, 8, 0)))
                .isInstanceOf(MemberInRosteredWindowException.class);
        assertThatThrownBy(() -> rina.remove(both, at(2026, 12, 5, 11, 0)))
                .isInstanceOf(MemberInRosteredWindowException.class);
        rina.remove(both, at(2026, 12, 5, 9, 15));

        assertThat(rina.isRemoved()).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A member rostered on nothing can be removed at any time, even during someone else's job.")
    void memberRosteredOnNothingCanAlwaysBeRemoved() {
        TeamMember siti = storedMember(SITI, TeamRole.HELPER, false, false);

        siti.remove(RosteredWindows.none(), at(2026, 12, 5, 11, 0));

        assertThat(siti.isRemoved()).isTrue();
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("A removed member is stored as removed and keeps when their verification falls due.")
    void restoredRemovedMemberKeepsItsState() {
        TeamMember removed = storedMember(SITI, TeamRole.HELPER, true, false, true);

        assertThat(removed.isRemoved()).isTrue();
        assertThat(removed.isDeactivated()).isTrue();
        assertThat(removed.isActive()).isFalse();
        assertThat(removed.verifiedUntil()).isEqualTo(VERIFIED_UNTIL);
        assertThat(storedMember(SITI, TeamRole.HELPER, false, false, false).isRemoved()).isFalse();
    }
}
