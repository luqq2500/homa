package com.homa.verification.usecase.ie;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.OWNER;
import static com.homa.verification.usecase.VerificationWorld.SITI_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.azmansAnswer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PayoutAccountHolderMatchedIE;
import com.homa.ie.events.PayoutAccountSubmittedIE;
import com.homa.verification.domain.IdentityNumber;
import com.homa.verification.domain.PersonName;
import com.homa.verification.usecase.FakeAccountHolderCheck;
import com.homa.verification.usecase.VerificationWorld;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandlePayoutAccountSubmittedIETest {

    private static final String PAYOUT_ACCOUNT = "payout-account-kemas-1";
    private static final Instant SUBMITTED_AT = at(2026, 10, 2, 11, 0);
    private static final Instant CHECKED_AT = at(2026, 10, 2, 11, 1);

    private final VerificationWorld world = new VerificationWorld();
    private String azman;

    @BeforeEach
    void azmanIsAVerifiedOwnerOfKemasHome() {
        azman = world.verify(AZMAN_SIGN_IN, OWNER, null, azmansAnswer()).personId();
        world.providerOwners.ownedBy(KEMAS_HOME_PROVIDER_ID, azman);
        world.publisher.clear();
        world.time.set(CHECKED_AT);
    }

    private static PayoutAccountSubmittedIE submitted(String eventId, String holder) {
        return new PayoutAccountSubmittedIE(eventId, KEMAS_HOME_PROVIDER_ID, PAYOUT_ACCOUNT, holder, SUBMITTED_AT);
    }

    private static PayoutAccountHolderMatchedIE matched(String holder, boolean matched) {
        return new PayoutAccountHolderMatchedIE("event-2", PAYOUT_ACCOUNT, KEMAS_HOME_PROVIDER_ID, holder, matched,
                CHECKED_AT);
    }

    @Test
    @Tag("PRV-2")
    @Tag("IDN-3")
    @DisplayName("Kemas Home's account in Azman's name is matched against his verified MyKad identity.")
    void anOwnerHeldAccountIsMatched() {
        world.holderCheck.accountHeldBy(PAYOUT_ACCOUNT, AZMAN_MYKAD);

        world.payoutAccountSubmitted.handle(submitted("payment-event-1", "OWNER"));

        assertThat(world.publisher.single(PayoutAccountHolderMatchedIE.class)).isEqualTo(matched("OWNER", true));
        assertThat(world.holderCheck.asked()).containsExactly(new FakeAccountHolderCheck.Question(PAYOUT_ACCOUNT,
                new IdentityNumber(AZMAN_MYKAD), new PersonName("Azman")));
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("An account the payment provider says isn't Azman's is not matched.")
    void anAccountHeldBySomeoneElseIsNotMatched() {
        world.holderCheck.accountHeldBy(PAYOUT_ACCOUNT, SITI_MYKAD);

        world.payoutAccountSubmitted.handle(submitted("payment-event-1", "OWNER"));

        assertThat(world.publisher.single(PayoutAccountHolderMatchedIE.class)).isEqualTo(matched("OWNER", false));
        assertThat(world.holderCheck.asked()).hasSize(1);
    }

    @Test
    @Tag("PRV-7")
    @DisplayName("A business-held account isn't matched in slice 1: there is no business verification to check it against.")
    void aBusinessHeldAccountIsNotMatchedYet() {
        world.holderCheck.accountHeldBy(PAYOUT_ACCOUNT, AZMAN_MYKAD);

        world.payoutAccountSubmitted.handle(submitted("payment-event-1", "BUSINESS"));

        assertThat(world.publisher.single(PayoutAccountHolderMatchedIE.class)).isEqualTo(matched("BUSINESS", false));
        assertThat(world.holderCheck.asked()).isEmpty();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Without a verified owner there's no identity to check the account against, so it isn't matched.")
    void anAccountWithoutAVerifiedOwnerIsNotMatched() {
        world.providerOwners.ownedBy("provider-hana-cleaning", "person-not-verified");
        world.holderCheck.accountHeldBy(PAYOUT_ACCOUNT, AZMAN_MYKAD);

        world.payoutAccountSubmitted.handle(new PayoutAccountSubmittedIE("payment-event-1", "provider-unknown",
                PAYOUT_ACCOUNT, "OWNER", SUBMITTED_AT));
        world.payoutAccountSubmitted.handle(new PayoutAccountSubmittedIE("payment-event-2", "provider-hana-cleaning",
                PAYOUT_ACCOUNT, "OWNER", SUBMITTED_AT));

        assertThat(world.publisher.published(PayoutAccountHolderMatchedIE.class))
                .extracting(PayoutAccountHolderMatchedIE::matched).containsExactly(false, false);
        assertThat(world.holderCheck.asked()).isEmpty();
    }

    @Test
    @Tag("PRV-10")
    @DisplayName("A redelivered submission changes nothing: the check is asked once and the outcome published once.")
    void aRedeliveredEventChangesNothing() {
        world.holderCheck.accountHeldBy(PAYOUT_ACCOUNT, AZMAN_MYKAD);
        PayoutAccountSubmittedIE event = submitted("payment-event-1", "OWNER");
        world.payoutAccountSubmitted.handle(event);

        world.payoutAccountSubmitted.handle(event);

        assertThat(world.publisher.published()).hasSize(1);
        assertThat(world.holderCheck.asked()).hasSize(1);
        assertThat(world.processedMessages.handled("payment-event-1", HandlePayoutAccountSubmittedIE.HANDLER)).isTrue();
        assertThat(HandlePayoutAccountSubmittedIE.HANDLER).isEqualTo("verification.PayoutAccountSubmitted");
    }

    @Test
    @Tag("PRV-10")
    @DisplayName("Kemas Home changes banks: the new account is checked afresh.")
    void everySubmissionIsCheckedAfresh() {
        world.holderCheck.accountHeldBy(PAYOUT_ACCOUNT, AZMAN_MYKAD);
        world.payoutAccountSubmitted.handle(submitted("payment-event-1", "OWNER"));

        world.payoutAccountSubmitted.handle(submitted("payment-event-2", "OWNER"));

        assertThat(world.publisher.published(PayoutAccountHolderMatchedIE.class))
                .extracting(PayoutAccountHolderMatchedIE::eventId).containsExactly("event-2", "event-3");
        assertThat(world.holderCheck.asked()).hasSize(2);
    }

    @Test
    @Tag("EXT-3")
    @DisplayName("When the payment provider can't answer, the handler fails so the relay retries it, and publishes nothing.")
    void aFailingCheckIsRetriedByTheRelay() {
        world.holderCheck.goDown();

        assertThatThrownBy(() -> world.payoutAccountSubmitted.handle(submitted("payment-event-1", "OWNER")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(world.publisher.published()).isEmpty();
    }
}
