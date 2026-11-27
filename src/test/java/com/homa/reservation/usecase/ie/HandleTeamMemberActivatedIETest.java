package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProfilePhotoChangedIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.usecase.ReservationTestBed;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleTeamMemberActivatedIETest {

    private final ReservationTestBed bed = new ReservationTestBed();

    private TeamMemberActivatedIE activated(String eventId, String memberId, String personId, String name,
            boolean photoVerified, String rosterableUntil) {
        return new TeamMemberActivatedIE(eventId, memberId, personId, KEMAS_HOME_PROVIDER_ID, name, "HELPER",
                rosterableUntil == null ? "LOCAL" : "FOREIGN_WORKER", photoVerified, rosterableUntil, TAPPED_PAY_AT);
    }

    private void photoChecked(String personId, boolean verified, String photoRef) {
        bed.profilePhotoChanged.handle(new ProfilePhotoChangedIE(bed.nextInboundId(), personId, verified, photoRef,
                TAPPED_PAY_AT));
    }

    private RosterableMember member(String memberId) {
        return bed.members.findById(memberId).orElseThrow();
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("Siti's photo was verified before she joined: she joins active, by her verified name, with that photo.")
    void memberJoinsWithThePhotoVerifiedBefore() {
        photoChecked("person-siti", true, "photo-siti");

        bed.teamMemberActivated.handle(activated("join-siti", SITI_MEMBER_ID, "person-siti", "Siti", true, null));

        assertThat(member(SITI_MEMBER_ID)).isEqualTo(new RosterableMember(SITI_MEMBER_ID, KEMAS_HOME_PROVIDER_ID,
                MemberRole.HELPER, "Siti", true, Optional.of("photo-siti"), true, Optional.empty()));
        assertThat(bed.members.findByPersonId("person-siti")).containsExactly(member(SITI_MEMBER_ID));
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("A verified photo Verification already reported wins over Provider's flag on joining.")
    void verifiedPhotoWinsOverTheActivationFlag() {
        photoChecked("person-siti", true, "photo-siti");

        bed.teamMemberActivated.handle(activated("join-siti", SITI_MEMBER_ID, "person-siti", "Siti", false, null));

        assertThat(member(SITI_MEMBER_ID).hasVerifiedPhoto()).isTrue();
        assertThat(member(SITI_MEMBER_ID).photoReference()).contains("photo-siti");
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("Nurul joins before Reservation hears of her photo: she can't be rostered until the verified photo arrives.")
    void memberWithoutAKnownPhotoWaitsForIt() {
        bed.teamMemberActivated.handle(activated("join-nurul", "member-nurul", "person-nurul", "Nurul", true, null));

        assertThat(member("member-nurul").photoVerified()).isTrue();
        assertThat(member("member-nurul").photoReference()).isEmpty();
        assertThat(member("member-nurul").hasVerifiedPhoto()).isFalse();

        photoChecked("person-nurul", true, "photo-nurul");
        assertThat(member("member-nurul").hasVerifiedPhoto()).isTrue();
    }

    @Test
    @Tag("ROS-4")
    @DisplayName("Rina's permit expires on 20 March, so Provider says she can work jobs ending by 20 December.")
    void foreignMemberKeepsTheLastDateProviderGives() {
        bed.teamMemberActivated.handle(activated("join-rina", RINA_MEMBER_ID, "person-rina", "Rina", true,
                "2026-12-20"));

        assertThat(member(RINA_MEMBER_ID).rosterableUntil()).contains(LocalDate.of(2026, 12, 20));
        assertThat(member(RINA_MEMBER_ID).role()).isEqualTo(MemberRole.HELPER);
        assertThat(member(RINA_MEMBER_ID).active()).isTrue();
    }

    @Test
    @Tag("ROS-3")
    @DisplayName("A redelivered activation changes nothing and records the member once.")
    void redeliveredActivationIsIgnored() {
        TeamMemberActivatedIE joined = activated("join-siti", SITI_MEMBER_ID, "person-siti", "Siti", true, null);
        bed.teamMemberActivated.handle(joined);
        photoChecked("person-siti", true, "photo-siti");

        bed.teamMemberActivated.handle(joined);

        assertThat(bed.members.count()).isEqualTo(1);
        assertThat(member(SITI_MEMBER_ID).photoReference()).contains("photo-siti");
        assertThat(HandleTeamMemberActivatedIE.HANDLER).isEqualTo("reservation.TeamMemberActivated");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
