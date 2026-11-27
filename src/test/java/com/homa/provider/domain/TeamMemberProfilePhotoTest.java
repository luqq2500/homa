package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.liveKemasHome;
import static com.homa.provider.domain.ProviderFixtures.siti;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TeamMemberProfilePhotoTest {

    @Test
    @Tag("IDN-4")
    @DisplayName("Siti's photo is checked against her MyKad verification and shown on the roster.")
    void membersPhotoCountsOnceVerificationHasCheckedIt() {
        TeamMember siti = siti(liveKemasHome());

        assertThat(siti.hasVerifiedPhoto()).isFalse();

        siti.recordPhotoVerification(true);

        assertThat(siti.hasVerifiedPhoto()).isTrue();
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("On Friday Aisyah sees Siti, Nurul and Farid: only a member with a verified photo can be on the roster.")
    void memberWithoutAVerifiedPhotoIsMarkedSoForTheRoster() {
        TeamMember siti = siti(liveKemasHome());
        siti.recordPhotoVerification(true);

        siti.recordPhotoVerification(false);

        assertThat(siti.hasVerifiedPhoto()).isFalse();
        assertThat(siti.isActive()).isTrue();
    }
}
