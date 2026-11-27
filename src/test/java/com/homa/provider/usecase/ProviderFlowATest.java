package com.homa.provider.usecase;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.AZMAN;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NURUL;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.azmanVerified;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.localMemberVerified;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.photoChanged;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.PayoutAccountVerifiedIE;
import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.ie.port.ProviderIE;
import com.homa.provider.domain.PayoutAccountHolder;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.usecase.dto.AddTeamMemberRequest;
import com.homa.provider.usecase.dto.AddTeamMemberResponse;
import com.homa.provider.usecase.dto.GoLiveRequest;
import com.homa.provider.usecase.dto.OpenProviderRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Provider's part of flow A (plan, "Key end-to-end flows"): before Aisyah can book Kemas Home, Azman verifies, opens
 * it, has its company payout account verified, adds Siti and Nurul, and takes it live.
 */
class ProviderFlowATest {

    private static final Instant GOES_LIVE_AT = HomaFixtures.at(2026, 9, 3, 9, 0);

    private final ProviderWorld world = new ProviderWorld();

    @Test
    @Tag("PRV-1")
    @Tag("PRV-2")
    @Tag("HLP-2")
    @Tag("IDN-4")
    @DisplayName("Azman is verified, opens Kemas Home, its payout account is verified, he adds Siti and Nurul, and it goes live.")
    void kemasHomeGoesLive() {
        world.personVerified.handle(azmanVerified("verification-azman"));
        String kemasHome = world.openProvider.open(new OpenProviderRequest(AZMAN)).providerId();

        world.payoutAccountVerified.handle(new PayoutAccountVerifiedIE("payout-kemas-home", kemasHome,
                "payout-account-kemas-home", "BUSINESS", true, HomaFixtures.at(2026, 9, 1, 10, 0)));
        assertThat(world.publisher.published()).isEmpty();

        world.personVerified.handle(localMemberVerified("verification-siti", SITI, "Siti"));
        world.personVerified.handle(localMemberVerified("verification-nurul", NURUL, "Nurul"));
        world.profilePhotoChanged.handle(photoChanged("photo-siti", SITI, true));
        world.profilePhotoChanged.handle(photoChanged("photo-nurul", NURUL, true));
        AddTeamMemberResponse siti = world.addTeamMember.add(
                new AddTeamMemberRequest(AZMAN, kemasHome, SITI, "HELPER", false));
        AddTeamMemberResponse nurul = world.addTeamMember.add(
                new AddTeamMemberRequest(AZMAN, kemasHome, NURUL, "HELPER", false));

        world.time.setNow(GOES_LIVE_AT);
        assertThat(world.goLive.goLive(new GoLiveRequest(AZMAN, kemasHome)).status()).isEqualTo("LIVE");

        assertThat(world.providers.stored(new ProviderId(kemasHome)).status()).isEqualTo(ProviderStatus.LIVE);
        assertThat(world.providers.stored(new ProviderId(kemasHome)).verifiedPayoutAccount())
                .contains(PayoutAccountHolder.BUSINESS);
        List<ProviderIE> published = world.publisher.published();
        assertThat(published).hasSize(3);
        assertThat(published.get(0)).isEqualTo(new TeamMemberActivatedIE("event-1", siti.memberId(), SITI, kemasHome,
                "Siti", "HELPER", "LOCAL", true, null, TEAM_ADDED_AT));
        assertThat(published.get(1)).isEqualTo(new TeamMemberActivatedIE("event-2", nurul.memberId(), NURUL,
                kemasHome, "Nurul", "HELPER", "LOCAL", true, null, TEAM_ADDED_AT));
        assertThat(published.get(2)).isEqualTo(
                new ProviderWentLiveIE("event-3", kemasHome, AZMAN, null, GOES_LIVE_AT));
    }
}
