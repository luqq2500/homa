package com.homa.verification.usecase;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.verification.usecase.VerificationWorld.AISYAH_CHECKED_AT;
import static com.homa.verification.usecase.VerificationWorld.AISYAH_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_CHECKED_AT;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.CUSTOMER;
import static com.homa.verification.usecase.VerificationWorld.MEMBER;
import static com.homa.verification.usecase.VerificationWorld.OWNER;
import static com.homa.verification.usecase.VerificationWorld.SITI_CHECKED_AT;
import static com.homa.verification.usecase.VerificationWorld.SITI_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.WEBHOOK_DELAY;
import static com.homa.verification.usecase.VerificationWorld.aisyahsAnswer;
import static com.homa.verification.usecase.VerificationWorld.azmansAnswer;
import static com.homa.verification.usecase.VerificationWorld.sitisAnswer;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.PayoutAccountHolderMatchedIE;
import com.homa.ie.events.PayoutAccountSubmittedIE;
import com.homa.ie.events.PersonVerifiedIE;
import com.homa.ie.port.VerificationIE;
import com.homa.verification.usecase.dto.StartVerificationResponse;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Flow A, as Verification sees it: Azman verifies as Kemas Home's owner and its owner-held payout account is
 * matched, so Kemas Home can go live (PRV-1, PRV-2); Siti verifies as a member; Aisyah verifies as a customer with
 * her MyKad at her first reservation, before paying (CUS-3, CUS-10).
 */
class FlowAVerificationTest {

    private static final String PAYOUT_ACCOUNT = "payout-account-kemas-1";
    private static final Instant PAYOUT_CHECKED_AT = at(2026, 10, 2, 11, 1);

    @Test
    @Tag("PRV-1")
    @Tag("PRV-2")
    @Tag("HLP-2")
    @Tag("CUS-10")
    @DisplayName("Flow A: Azman, Kemas Home's payout account, Siti, then Aisyah before she pays on Monday 20:00.")
    void flowA() {
        VerificationWorld world = new VerificationWorld();

        StartVerificationResponse azman = world.start(AZMAN_SIGN_IN, OWNER, null, "MyKad");
        assertThat(world.answer(azman, azmansAnswer()).outcome()).isEqualTo("VERIFIED");

        world.providerOwners.ownedBy(KEMAS_HOME_PROVIDER_ID, azman.personId());
        world.holderCheck.accountHeldBy(PAYOUT_ACCOUNT, AZMAN_MYKAD);
        world.time.set(PAYOUT_CHECKED_AT);
        world.payoutAccountSubmitted.handle(new PayoutAccountSubmittedIE("payment-event-1", KEMAS_HOME_PROVIDER_ID,
                PAYOUT_ACCOUNT, "OWNER", at(2026, 10, 2, 11, 0)));

        StartVerificationResponse siti = world.start(SITI_SIGN_IN, MEMBER, null, "MyKad");
        assertThat(world.answer(siti, sitisAnswer()).outcome()).isEqualTo("VERIFIED");

        StartVerificationResponse aisyah = world.start(AISYAH_SIGN_IN, CUSTOMER, HomaFixtures.AISYAH_CUSTOMER_ID,
                "MyKad");
        assertThat(world.answer(aisyah, aisyahsAnswer()).outcome()).isEqualTo("VERIFIED");
        assertThat(AISYAH_CHECKED_AT.plus(WEBHOOK_DELAY)).isBefore(HomaFixtures.REQUESTED_AT);

        assertThat(world.publisher.published()).<VerificationIE>containsExactly(
                new PersonVerifiedIE("event-1", azman.personId(), "OWNER", null, "Azman", "LOCAL", true, null,
                        at(2027, 4, 1, 9, 0), AZMAN_CHECKED_AT.plus(WEBHOOK_DELAY)),
                new PayoutAccountHolderMatchedIE("event-2", PAYOUT_ACCOUNT, KEMAS_HOME_PROVIDER_ID, "OWNER", true,
                        PAYOUT_CHECKED_AT),
                new PersonVerifiedIE("event-3", siti.personId(), "MEMBER", null, "Siti", "LOCAL", true, null,
                        at(2027, 5, 20, 10, 0), SITI_CHECKED_AT.plus(WEBHOOK_DELAY)),
                new PersonVerifiedIE("event-4", aisyah.personId(), "CUSTOMER", HomaFixtures.AISYAH_CUSTOMER_ID,
                        HomaFixtures.AISYAH_NAME, "LOCAL", true, null, null, AISYAH_CHECKED_AT.plus(WEBHOOK_DELAY)));
        assertThat(world.persons.size()).isEqualTo(3);
    }
}
