package com.homa.provider.usecase.ie;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.KEMAS_HOME;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.kemasHome;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.payoutAccountVerified;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.PayoutAccountVerifiedIE;
import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.provider.domain.Eligibility.Local;
import com.homa.provider.domain.PayoutAccountHolder;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.usecase.ProviderWorld;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandlePayoutAccountVerifiedIETest {

    private static final Instant HANDLED_AT = HomaFixtures.at(2026, 10, 8, 11, 0);

    private final ProviderWorld world = new ProviderWorld();

    HandlePayoutAccountVerifiedIETest() {
        world.time.setNow(HANDLED_AT);
    }

    private void sitiIsAHelper(boolean deactivated) {
        world.members.save(TeamMember.restore(new TeamMemberId("member-siti"), KEMAS_HOME, new PersonId(SITI),
                TeamRole.HELPER, new Local(), TEAM_ADDED_AT, true, deactivated, false));
    }

    @Test
    @Tag("PRV-2")
    @Tag("PRV-7")
    @DisplayName("Kemas Home's company account is verified before it goes live: it can now go live, and nothing is published.")
    void verifiedAccountBeforeGoingLiveIsRecordedQuietly() {
        world.providers.save(kemasHome(ProviderStatus.OPENED, null));
        sitiIsAHelper(false);

        world.payoutAccountVerified.handle(payoutAccountVerified("payout-1", "BUSINESS", true));

        assertThat(world.providers.stored(KEMAS_HOME).verifiedPayoutAccount()).contains(PayoutAccountHolder.BUSINESS);
        assertThat(world.processedMessages.hasHandled("payout-1", "provider.PayoutAccountVerified")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("An account in the owner's name counts too.")
    void ownersAccountCounts() {
        world.providers.save(kemasHome(ProviderStatus.OPENED, null));

        world.payoutAccountVerified.handle(payoutAccountVerified("payout-2", "OWNER", true));

        assertThat(world.providers.stored(KEMAS_HOME).verifiedPayoutAccount()).contains(PayoutAccountHolder.OWNER);
    }

    @Test
    @Tag("PRV-2")
    @Tag("PRV-10")
    @DisplayName("Live Kemas Home's new account fails the check: it stops being bookable, and ProviderBookabilityChangedIE says why.")
    void liveProviderLosingItsAccountStopsBeingBookable() {
        world.providers.save(kemasHome(ProviderStatus.LIVE, PayoutAccountHolder.BUSINESS));
        sitiIsAHelper(false);

        world.payoutAccountVerified.handle(payoutAccountVerified("payout-3", "BUSINESS", false));

        assertThat(world.providers.stored(KEMAS_HOME).verifiedPayoutAccount()).isEmpty();
        ProviderBookabilityChangedIE event = world.publisher.single(ProviderBookabilityChangedIE.class);
        assertThat(event.eventId()).isEqualTo("event-1");
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(event.bookable()).isFalse();
        assertThat(event.reason()).isEqualTo("NO_VERIFIED_PAYOUT_ACCOUNT");
        assertThat(event.occurredAt()).isEqualTo(HANDLED_AT);
    }

    @Test
    @Tag("PRV-2")
    @Tag("PRV-10")
    @DisplayName("When live Kemas Home's new account is verified, it is bookable again, with no reason.")
    void liveProviderRegainingAnAccountIsBookableAgain() {
        world.providers.save(kemasHome(ProviderStatus.LIVE, null));
        sitiIsAHelper(false);

        world.payoutAccountVerified.handle(payoutAccountVerified("payout-4", "BUSINESS", true));

        ProviderBookabilityChangedIE event = world.publisher.single(ProviderBookabilityChangedIE.class);
        assertThat(event.eventId()).isEqualTo("event-1");
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(event.bookable()).isTrue();
        assertThat(event.reason()).isNull();
        assertThat(event.occurredAt()).isEqualTo(HANDLED_AT);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("With every helper deactivated, a verified account still leaves it unbookable, now for want of helpers.")
    void verifiedAccountWithoutActiveHelpersChangesTheReason() {
        world.providers.save(kemasHome(ProviderStatus.LIVE, null));
        sitiIsAHelper(true);

        world.payoutAccountVerified.handle(payoutAccountVerified("payout-5", "BUSINESS", true));

        ProviderBookabilityChangedIE event = world.publisher.single(ProviderBookabilityChangedIE.class);
        assertThat(event.bookable()).isFalse();
        assertThat(event.reason()).isEqualTo("NO_ACTIVE_HELPERS");
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A live, bookable provider whose account is verified again stays bookable, and nothing is published.")
    void unchangedBookabilityPublishesNothing() {
        world.providers.save(kemasHome(ProviderStatus.LIVE, PayoutAccountHolder.BUSINESS));
        sitiIsAHelper(false);

        world.payoutAccountVerified.handle(payoutAccountVerified("payout-6", "OWNER", true));

        assertThat(world.providers.stored(KEMAS_HOME).verifiedPayoutAccount()).contains(PayoutAccountHolder.OWNER);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("An event for a provider Provider doesn't know is recorded and dropped, so it isn't retried forever.")
    void unknownProviderIsDropped() {
        PayoutAccountVerifiedIE stray = new PayoutAccountVerifiedIE("payout-7", "provider-unknown", "payout-account-x",
                "OWNER", true, HANDLED_AT);

        world.payoutAccountVerified.handle(stray);

        assertThat(world.providers.count()).isZero();
        assertThat(world.processedMessages.hasHandled("payout-7", "provider.PayoutAccountVerified")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A redelivered PayoutAccountVerifiedIE changes nothing and publishes nothing.")
    void redeliveryChangesNothing() {
        world.providers.save(kemasHome(ProviderStatus.LIVE, PayoutAccountHolder.BUSINESS));
        sitiIsAHelper(false);
        PayoutAccountVerifiedIE failed = payoutAccountVerified("payout-8", "BUSINESS", false);
        world.payoutAccountVerified.handle(failed);
        world.payoutAccountVerified.handle(payoutAccountVerified("payout-9", "BUSINESS", true));

        world.payoutAccountVerified.handle(failed);

        assertThat(world.providers.stored(KEMAS_HOME).verifiedPayoutAccount()).contains(PayoutAccountHolder.BUSINESS);
        assertThat(world.publisher.published()).hasSize(2);
    }
}
