package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.domain.ReservationFixtures.farid;
import static com.homa.reservation.domain.ReservationFixtures.nurul;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.exception.IncompleteRosterException;
import com.homa.reservation.domain.roster.Roster;
import com.homa.reservation.domain.roster.RosterMember;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RosterTest {

    private static final List<RosterMember> SITI_AND_NURUL =
            List.of(siti().asRosterMember(), nurul().asRosterMember());

    private static Roster sitiAndNurul() {
        return Roster.of(SITI_AND_NURUL, 2);
    }

    @Test
    @Tag("ROS-2")
    @DisplayName("Siti and Nurul for 2 helpers is a complete roster that nobody has to replace and nothing has locked.")
    void aRosterTheProviderSetsIsCompleteAndOpen() {
        Roster roster = sitiAndNurul();

        assertThat(roster.members()).containsExactlyElementsOf(SITI_AND_NURUL);
        assertThat(roster.isComplete()).isTrue();
        assertThat(roster.membersToReplace()).isEmpty();
        assertThat(roster.isLocked()).isFalse();
        assertThat(roster.lockedAt()).isEmpty();
    }

    @Test
    @Tag("ROS-2")
    @DisplayName("Siti, Nurul and Farid is complete for 2 helpers; Siti twice, or one helper short, isn't a roster.")
    void aRosterNeedsTheBookedHelpersEachOnce() {
        assertThat(Roster.of(List.of(siti().asRosterMember(), nurul().asRosterMember(), farid().asRosterMember()), 2)
                .helpers()).hasSize(2);
        assertThatThrownBy(() -> Roster.of(List.of(siti().asRosterMember(), siti().asRosterMember()), 2))
                .isInstanceOf(IncompleteRosterException.class);
        assertThatThrownBy(() -> Roster.of(List.of(siti().asRosterMember(), farid().asRosterMember()), 2))
                .isInstanceOf(IncompleteRosterException.class);
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Flagging Nurul leaves the roster as it was and gives a roster that needs fixing; flagging doesn't change the first one.")
    void flaggingAMemberGivesARosterThatNeedsFixingAndLeavesTheOriginalAlone() {
        Roster roster = sitiAndNurul();

        Roster flagged = roster.flaggedForReplacement(NURUL_MEMBER_ID);

        assertThat(flagged.members()).containsExactlyElementsOf(SITI_AND_NURUL);
        assertThat(flagged.membersToReplace()).containsExactly(NURUL_MEMBER_ID);
        assertThat(flagged.isComplete()).isFalse();
        assertThat(roster.isComplete()).isTrue();
        assertThat(flagged.flaggedForReplacement(SITI_MEMBER_ID).membersToReplace())
                .containsExactlyInAnyOrder(NURUL_MEMBER_ID, SITI_MEMBER_ID);
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Only a member on the roster who isn't flagged yet can be flagged.")
    void onlyAnUnflaggedMemberOnTheRosterCanBeFlagged() {
        Roster flagged = sitiAndNurul().flaggedForReplacement(NURUL_MEMBER_ID);

        assertThat(sitiAndNurul().canFlagForReplacement(NURUL_MEMBER_ID)).isTrue();
        assertThat(flagged.canFlagForReplacement(SITI_MEMBER_ID)).isTrue();
        assertThat(flagged.canFlagForReplacement(NURUL_MEMBER_ID)).isFalse();
        assertThat(flagged.canFlagForReplacement(RINA_MEMBER_ID)).isFalse();
        assertThat(sitiAndNurul().includes(SITI_MEMBER_ID)).isTrue();
        assertThat(sitiAndNurul().includes(RINA_MEMBER_ID)).isFalse();
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("The roster locks at 9:45, and locking it keeps its members and its record of when.")
    void lockingARosterRecordsWhenAndKeepsItsMembers() {
        Roster roster = sitiAndNurul();

        Roster locked = roster.locked(saturday(9, 45));

        assertThat(locked.isLocked()).isTrue();
        assertThat(locked.lockedAt()).contains(saturday(9, 45));
        assertThat(locked.members()).containsExactlyElementsOf(SITI_AND_NURUL);
        assertThat(roster.isLocked()).isFalse();
        assertThat(locked.membersToReplace()).isEmpty();
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("A flagged roster keeps its flags and its lock when the other changes.")
    void flagsAndLockSurviveTheOtherChange() {
        Roster flagged = sitiAndNurul().flaggedForReplacement(NURUL_MEMBER_ID);
        Roster lockedAfterwards = sitiAndNurul().locked(saturday(9, 45)).flaggedForReplacement(NURUL_MEMBER_ID);

        assertThat(flagged.lockedAt()).isEmpty();
        assertThat(lockedAfterwards.lockedAt()).contains(saturday(9, 45));
        assertThat(lockedAfterwards.membersToReplace()).containsExactly(NURUL_MEMBER_ID);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Siti is rostered, so she acts for Kemas Home on it; Rina isn't.")
    void aPersonIsRosteredWhenOneOfItsMembersIsThem() {
        Roster roster = sitiAndNurul();

        assertThat(roster.includesPerson("person-siti")).isTrue();
        assertThat(roster.includesPerson("person-nurul")).isTrue();
        assertThat(roster.includesPerson("person-rina")).isFalse();
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Persistence: a roster comes back as it was stored, with who is to be replaced and when it locked.")
    void aStoredRosterComesBackAsItWasStored() {
        Roster flagged = Roster.restore(SITI_AND_NURUL, Set.of(NURUL_MEMBER_ID), null);
        Roster locked = Roster.restore(SITI_AND_NURUL, Set.of(), saturday(9, 45));

        assertThat(flagged.membersToReplace()).containsExactly(NURUL_MEMBER_ID);
        assertThat(flagged.lockedAt()).isEmpty();
        assertThat(flagged.members()).containsExactlyElementsOf(SITI_AND_NURUL);
        assertThat(locked.lockedAt()).isEqualTo(Optional.of(saturday(9, 45)));
        assertThat(locked.isComplete()).isTrue();
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Persistence: a stored roster with a member twice, someone to replace who isn't on it, or someone to replace once locked, is refused.")
    void aStoredRosterThatIsNotConsistentIsRefused() {
        assertThatThrownBy(() -> Roster.restore(List.of(siti().asRosterMember(), siti().asRosterMember()), Set.of(),
                null)).isInstanceOf(IllegalArgumentException.class).hasMessage("A stored roster needs each member once");
        assertThatThrownBy(() -> Roster.restore(SITI_AND_NURUL, Set.of(RINA_MEMBER_ID), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A stored roster needs the members to replace on the roster");
        assertThatThrownBy(() -> Roster.restore(SITI_AND_NURUL, Set.of(NURUL_MEMBER_ID), saturday(9, 45)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A stored roster needs nobody to replace once it is locked");
    }
}
