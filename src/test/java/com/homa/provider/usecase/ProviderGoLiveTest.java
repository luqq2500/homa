package com.homa.provider.usecase;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.AZMAN;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.FARID;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.HANA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.KEMAS_HOME;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.kemasHome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.provider.domain.Eligibility.Local;
import com.homa.provider.domain.PayoutAccountHolder;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.domain.exception.NoActiveHelperException;
import com.homa.provider.domain.exception.NoVerifiedPayoutAccountException;
import com.homa.provider.domain.exception.ProviderAlreadyLiveException;
import com.homa.provider.usecase.dto.GoLiveRequest;
import com.homa.provider.usecase.dto.GoLiveResponse;
import com.homa.provider.usecase.exception.NotAllowedException;
import com.homa.provider.usecase.exception.ProviderNotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderGoLiveTest {

    private static final Instant GOES_LIVE_AT = HomaFixtures.at(2026, 9, 3, 9, 0);

    private final ProviderWorld world = new ProviderWorld();

    private void member(ProviderId provider, String id, String personId, TeamRole role) {
        world.members.save(TeamMember.restore(new TeamMemberId(id), provider, new PersonId(personId), role,
                new Local(), TEAM_ADDED_AT, true, false, false));
    }

    private GoLiveResponse azmanGoesLive() {
        return world.goLive.goLive(new GoLiveRequest(AZMAN, KEMAS_HOME.value()));
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home, with its payout account verified and Siti as a helper, goes live and ProviderWentLiveIE says so.")
    void providerWithVerifiedPayoutAccountAndAHelperGoesLive() {
        world.providers.save(kemasHome(ProviderStatus.OPENED, PayoutAccountHolder.BUSINESS));
        member(KEMAS_HOME, "member-siti", SITI, TeamRole.HELPER);
        world.time.setNow(GOES_LIVE_AT);

        GoLiveResponse response = azmanGoesLive();

        assertThat(response.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(response.status()).isEqualTo("LIVE");
        assertThat(world.providers.stored(KEMAS_HOME).status()).isEqualTo(ProviderStatus.LIVE);
        ProviderWentLiveIE event = world.publisher.single(ProviderWentLiveIE.class);
        assertThat(event.eventId()).isEqualTo("event-1");
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(event.ownerPersonId()).isEqualTo(AZMAN);
        assertThat(event.businessRegistrationNo()).isNull();
        assertThat(event.occurredAt()).isEqualTo(GOES_LIVE_AT);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home can't go live until its payout account is verified.")
    void providerWithoutVerifiedPayoutAccountStaysOpened() {
        world.providers.save(kemasHome(ProviderStatus.OPENED, null));
        member(KEMAS_HOME, "member-siti", SITI, TeamRole.HELPER);

        assertThatThrownBy(this::azmanGoesLive).isInstanceOf(NoVerifiedPayoutAccountException.class);
        assertThat(world.providers.stored(KEMAS_HOME).status()).isEqualTo(ProviderStatus.OPENED);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @Tag("PRV-3")
    @DisplayName("A supervisor alone isn't enough: Kemas Home needs an active helper to go live.")
    void supervisorAloneIsNotEnough() {
        world.providers.save(kemasHome(ProviderStatus.OPENED, PayoutAccountHolder.BUSINESS));
        member(KEMAS_HOME, "member-farid", FARID, TeamRole.SUPERVISOR);

        assertThatThrownBy(this::azmanGoesLive).isInstanceOf(NoActiveHelperException.class);
        assertThat(world.providers.stored(KEMAS_HOME).status()).isEqualTo(ProviderStatus.OPENED);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Another provider's helpers don't count towards Kemas Home's.")
    void anotherProvidersHelpersDoNotCount() {
        world.providers.save(kemasHome(ProviderStatus.OPENED, PayoutAccountHolder.BUSINESS));
        member(new ProviderId("provider-hana-cleaning"), "member-hana", HANA, TeamRole.HELPER);

        assertThatThrownBy(this::azmanGoesLive).isInstanceOf(NoActiveHelperException.class);
    }

    @Test
    @DisplayName("A provider that is already live can't go live again, and nothing is published.")
    void liveProviderCannotGoLiveAgain() {
        world.providers.save(kemasHome(ProviderStatus.LIVE, PayoutAccountHolder.BUSINESS));
        member(KEMAS_HOME, "member-siti", SITI, TeamRole.HELPER);

        assertThatThrownBy(this::azmanGoesLive).isInstanceOf(ProviderAlreadyLiveException.class);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-1")
    @Tag("PRV-11")
    @DisplayName("Siti, a helper, can't take Kemas Home live: only Azman runs its account.")
    void onlyTheOwnerTakesTheProviderLive() {
        world.providers.save(kemasHome(ProviderStatus.OPENED, PayoutAccountHolder.BUSINESS));
        member(KEMAS_HOME, "member-siti", SITI, TeamRole.HELPER);

        assertThatThrownBy(() -> world.goLive.goLive(new GoLiveRequest(SITI, KEMAS_HOME.value())))
                .isInstanceOf(NotAllowedException.class);
        assertThat(world.providers.stored(KEMAS_HOME).status()).isEqualTo(ProviderStatus.OPENED);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A provider that doesn't exist can't go live.")
    void unknownProviderIsNotFound() {
        assertThatThrownBy(() -> world.goLive.goLive(new GoLiveRequest(AZMAN, "provider-unknown")))
                .isInstanceOf(ProviderNotFoundException.class);
    }
}
