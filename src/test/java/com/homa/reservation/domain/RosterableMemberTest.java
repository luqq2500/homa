package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.domain.ReservationFixtures.deactivated;
import static com.homa.reservation.domain.ReservationFixtures.rina;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static com.homa.reservation.domain.ReservationFixtures.withoutVerifiedPhoto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.replica.PersonPhoto;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.roster.MemberRole;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RosterableMemberTest {

    @Test
    @Tag("ROS-3")
    @DisplayName("Siti goes on leave and is deactivated, so she can't be rostered until she is reactivated.")
    void onlyAnActiveMemberCanWorkOnAJob() {
        assertThat(siti().canWorkOn(JOB_END)).isTrue();
        assertThat(deactivated(siti()).canWorkOn(JOB_END)).isFalse();
    }

    @Test
    @Tag("ROS-4")
    @DisplayName("Rina's permit expires on 20 March, so she can work jobs up to 20 December, not 2 January.")
    void foreignMemberWorksOnlyOnJobsEndingByTheDateProviderGives() {
        assertThat(rina().rosterableUntil()).contains(LocalDate.of(2026, 12, 20));
        assertThat(rina().canWorkOn(HomaFixtures.at(2026, 12, 20, 23, 59))).isTrue();
        assertThat(rina().canWorkOn(HomaFixtures.at(2026, 12, 21, 0, 0))).isFalse();
        assertThat(rina().canWorkOn(HomaFixtures.at(2027, 1, 2, 14, 0))).isFalse();
        assertThat(deactivated(rina()).canWorkOn(JOB_END)).isFalse();
    }

    @Test
    @Tag("ROS-4")
    @DisplayName("Siti is local and needs no permit, so any job date is fine for her.")
    void localMemberHasNoLastDate() {
        assertThat(siti().rosterableUntil()).isEmpty();
        assertThat(siti().canWorkOn(HomaFixtures.at(2099, 12, 31, 23, 0))).isTrue();
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("A member belongs to its provider and is shown on the roster by name and verified photo.")
    void memberBelongsToItsProviderAndAppearsOnTheRosterByNameAndPhoto() {
        assertThat(siti().worksFor(KEMAS_HOME_PROVIDER_ID)).isTrue();
        assertThat(siti().worksFor("provider-another")).isFalse();
        assertThat(siti().asRosterMember().name()).isEqualTo("Siti");
        assertThat(siti().asRosterMember().photoReference()).isEqualTo("photo-siti");
        assertThat(siti().asRosterMember().isHelper()).isTrue();
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("Siti's photo is checked against her MyKad verification and shown on the roster.")
    void memberWithAVerifiedPhotoCanBeRostered() {
        assertThat(siti().photoVerified()).isTrue();
        assertThat(siti().hasVerifiedPhoto()).isTrue();
        assertThat(siti().canWorkOn(JOB_END)).isTrue();
        assertThat(siti().asRosterMember().photoReference()).isEqualTo("photo-siti");
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("When she uploads a new one, her old photo stays until the new one is verified.")
    void newPhotoReplacesTheRosterPhotoOnlyOnceVerified() {
        RosterableMember siti = siti();

        RosterableMember afterNewPhotoVerified = siti.withVerifiedPhoto("photo-siti-new");

        assertThat(siti.photoReference()).contains("photo-siti");
        assertThat(afterNewPhotoVerified.photoReference()).contains("photo-siti-new");
        assertThat(afterNewPhotoVerified.asRosterMember().photoReference()).isEqualTo("photo-siti-new");
        assertThat(afterNewPhotoVerified).isEqualTo(new RosterableMember(siti.memberId(), siti.personId(),
                siti.providerId(), siti.role(), siti.name(), true, Optional.of("photo-siti-new"), siti.active(),
                siti.rosterableUntil()));
        assertThat(rina().withVerifiedPhoto("photo-rina-new").rosterableUntil()).isEqualTo(rina().rosterableUntil());
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("Siti's new photo fails the check, so the roster keeps her old verified photo; once one passes, it replaces it.")
    void personPhotoChangesOnlyWhenTheNewPhotoIsVerified() {
        PersonPhoto none = PersonPhoto.none("person-siti");
        PersonPhoto verified = none.checked(true, "photo-siti-new");

        assertThat(none.verifiedReference()).isEmpty();
        assertThat(none.checked(false, "photo-siti-blurry")).isEqualTo(none);
        assertThat(verified).isEqualTo(new PersonPhoto("person-siti", Optional.of("photo-siti-new")));
        assertThat(verified.checked(false, "photo-siti-blurry")).isEqualTo(verified);
        assertThat(verified.checked(true, "photo-siti-newer").verifiedReference()).contains("photo-siti-newer");

        assertThat(siti().showing(none)).isEqualTo(siti());
        assertThat(siti().showing(verified)).isEqualTo(siti().withVerifiedPhoto("photo-siti-new"));
        assertThat(withoutVerifiedPhoto(siti()).showing(verified).canWorkOn(JOB_END)).isTrue();
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("A member without a verified photo can't be rostered, because the customer must see each rostered member's photo.")
    void memberWithoutAVerifiedPhotoCannotBeRostered() {
        RosterableMember noPhoto = withoutVerifiedPhoto(siti());
        RosterableMember unverifiedPhoto = new RosterableMember(SITI_MEMBER_ID, "person-siti", KEMAS_HOME_PROVIDER_ID,
                MemberRole.HELPER, "Siti", false, Optional.of("photo-siti"), true, Optional.empty());
        RosterableMember verifiedButUnknown = new RosterableMember(SITI_MEMBER_ID, "person-siti",
                KEMAS_HOME_PROVIDER_ID, MemberRole.HELPER, "Siti", true, Optional.empty(), true, Optional.empty());

        assertThat(noPhoto.hasVerifiedPhoto()).isFalse();
        assertThat(noPhoto.canWorkOn(JOB_END)).isFalse();
        assertThat(unverifiedPhoto.hasVerifiedPhoto()).isFalse();
        assertThat(unverifiedPhoto.canWorkOn(JOB_END)).isFalse();
        assertThat(verifiedButUnknown.hasVerifiedPhoto()).isFalse();
        assertThat(verifiedButUnknown.canWorkOn(JOB_END)).isFalse();
        assertThatThrownBy(unverifiedPhoto::asRosterMember).isInstanceOf(MemberNotRosterableException.class);
        assertThat(noPhoto.withVerifiedPhoto("photo-siti").canWorkOn(JOB_END)).isTrue();
    }
}
