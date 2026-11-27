package com.homa.provider.usecase.ie;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.KEMAS_HOME;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NURUL;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.photoChanged;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProfilePhotoChangedIE;
import com.homa.provider.domain.Eligibility.Local;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.usecase.ProviderWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProfilePhotoChangedIETest {

    private static final TeamMemberId SITI_MEMBER = new TeamMemberId("member-siti");
    private static final TeamMemberId NURUL_MEMBER = new TeamMemberId("member-nurul");

    private final ProviderWorld world = new ProviderWorld();

    private void helper(TeamMemberId id, String personId, boolean photoVerified) {
        world.members.save(TeamMember.restore(id, KEMAS_HOME, new PersonId(personId), TeamRole.HELPER, new Local(),
                TEAM_ADDED_AT, photoVerified, false, false));
    }

    @Test
    @Tag("IDN-4")
    @Tag("ROS-6")
    @DisplayName("Siti's photo is verified against her MyKad: her membership records it, Nurul's doesn't change, and nothing is published.")
    void membersPhotoBecomesVerified() {
        helper(SITI_MEMBER, SITI, false);
        helper(NURUL_MEMBER, NURUL, false);

        world.profilePhotoChanged.handle(photoChanged("photo-1", SITI, true));

        assertThat(world.members.stored(SITI_MEMBER).hasVerifiedPhoto()).isTrue();
        assertThat(world.members.stored(NURUL_MEMBER).hasVerifiedPhoto()).isFalse();
        assertThat(world.photos.photoVerifiedOf(new PersonId(SITI))).contains(true);
        assertThat(world.processedMessages.hasHandled("photo-1", "provider.ProfilePhotoChanged")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("When Verification reports Siti has no verified photo, her membership records that too.")
    void membersPhotoStopsBeingVerified() {
        helper(SITI_MEMBER, SITI, true);

        world.profilePhotoChanged.handle(photoChanged("photo-2", SITI, false));

        assertThat(world.members.stored(SITI_MEMBER).hasVerifiedPhoto()).isFalse();
        assertThat(world.photos.photoVerifiedOf(new PersonId(SITI))).contains(false);
    }

    @Test
    @Tag("IDN-4")
    @DisplayName("Nurul's photo is verified before she joins: the fact is kept for when she is added.")
    void photoOfSomeoneNotYetAMemberIsKept() {
        world.profilePhotoChanged.handle(photoChanged("photo-3", NURUL, true));

        assertThat(world.photos.photoVerifiedOf(new PersonId(NURUL))).contains(true);
        assertThat(world.members.all()).isEmpty();
    }

    @Test
    @DisplayName("A redelivered ProfilePhotoChangedIE changes nothing and publishes nothing.")
    void redeliveryChangesNothing() {
        helper(SITI_MEMBER, SITI, false);
        ProfilePhotoChangedIE verified = photoChanged("photo-4", SITI, true);
        world.profilePhotoChanged.handle(verified);
        world.profilePhotoChanged.handle(photoChanged("photo-5", SITI, false));

        world.profilePhotoChanged.handle(verified);

        assertThat(world.members.stored(SITI_MEMBER).hasVerifiedPhoto()).isFalse();
        assertThat(world.photos.photoVerifiedOf(new PersonId(SITI))).contains(false);
        assertThat(world.publisher.published()).isEmpty();
    }
}
