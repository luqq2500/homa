package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProfilePhotoChangedIE;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.replica.PersonPhoto;
import com.homa.reservation.domain.roster.RosterMember;
import com.homa.reservation.usecase.ReservationTestBed;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProfilePhotoChangedIETest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private ProfilePhotoChangedIE checked(String personId, boolean verified, String photoRef) {
        return new ProfilePhotoChangedIE(bed.nextInboundId(), personId, verified, photoRef, TAPPED_PAY_AT);
    }

    private Optional<String> photoOf(String memberId) {
        return bed.members.findById(memberId).orElseThrow().photoReference();
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("Siti's new photo is verified, so it replaces the one new rosters show.")
    void verifiedPhotoReplacesTheShownOne() {
        bed.profilePhotoChanged.handle(checked("person-siti", true, "photo-siti-new"));

        assertThat(photoOf(SITI_MEMBER_ID)).contains("photo-siti-new");
        assertThat(bed.photos.findByPersonId("person-siti"))
                .contains(new PersonPhoto("person-siti", Optional.of("photo-siti-new")));
        assertThat(photoOf(NURUL_MEMBER_ID)).contains("photo-nurul");
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("When she uploads a new one, her old photo stays until the new one is verified.")
    void unverifiedPhotoChangesNothing() {
        bed.profilePhotoChanged.handle(checked("person-siti", false, "photo-siti-blurry"));

        assertThat(photoOf(SITI_MEMBER_ID)).contains("photo-siti");
        assertThat(bed.photos.findByPersonId("person-siti"))
                .contains(new PersonPhoto("person-siti", Optional.of("photo-siti")));
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("A photo checked before the person joins any team is kept for when they do.")
    void photoOfAPersonNotYetAMemberIsKept() {
        bed.profilePhotoChanged.handle(checked("person-aina", true, "photo-aina"));

        assertThat(bed.photos.findByPersonId("person-aina"))
                .contains(new PersonPhoto("person-aina", Optional.of("photo-aina")));
        assertThat(bed.members.findByPersonId("person-aina")).isEmpty();
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("Farid's roster set on Friday keeps the photo it was set with; new rosters show his new verified photo.")
    void rosterAlreadySetKeepsItsPhoto() {
        Reservation reserved = bed.reserveStandard();
        bed.time.set(friday(18, 0));
        bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), KEMAS_HOME_PROVIDER_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID, FARID_MEMBER_ID)));

        bed.profilePhotoChanged.handle(checked("person-farid", true, "photo-farid-new"));

        assertThat(bed.reload(reserved).roster().orElseThrow().members()).extracting(RosterMember::photoReference)
                .containsExactly("photo-siti", "photo-nurul", "photo-farid");
        assertThat(photoOf(FARID_MEMBER_ID)).contains("photo-farid-new");
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("A redelivered photo check changes nothing, even after a newer verified photo.")
    void redeliveredPhotoCheckIsIgnored() {
        ProfilePhotoChangedIE first = checked("person-siti", true, "photo-siti-2");
        bed.profilePhotoChanged.handle(first);
        bed.profilePhotoChanged.handle(checked("person-siti", true, "photo-siti-3"));

        bed.profilePhotoChanged.handle(first);

        assertThat(photoOf(SITI_MEMBER_ID)).contains("photo-siti-3");
        assertThat(HandleProfilePhotoChangedIE.HANDLER).isEqualTo("reservation.ProfilePhotoChanged");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
