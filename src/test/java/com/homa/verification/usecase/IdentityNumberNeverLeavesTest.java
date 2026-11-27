package com.homa.verification.usecase;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.verification.usecase.VerificationWorld.AISYAH_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.AISYAH_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.CUSTOMER;
import static com.homa.verification.usecase.VerificationWorld.DANIEL_CUSTOMER_ID;
import static com.homa.verification.usecase.VerificationWorld.DANIEL_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.DANIEL_SECOND_CUSTOMER_ID;
import static com.homa.verification.usecase.VerificationWorld.DANIEL_SECOND_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.DANIEL_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.MEMBER;
import static com.homa.verification.usecase.VerificationWorld.OWNER;
import static com.homa.verification.usecase.VerificationWorld.RINA_IKAD;
import static com.homa.verification.usecase.VerificationWorld.RINA_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.SITI_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.SITI_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.SON_CUSTOMER_ID;
import static com.homa.verification.usecase.VerificationWorld.SON_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.SON_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.aisyahsAnswer;
import static com.homa.verification.usecase.VerificationWorld.azmansAnswer;
import static com.homa.verification.usecase.VerificationWorld.danielsAnswer;
import static com.homa.verification.usecase.VerificationWorld.rinasAnswer;
import static com.homa.verification.usecase.VerificationWorld.sitisAnswer;
import static com.homa.verification.usecase.VerificationWorld.sonsAnswer;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.PayoutAccountHolderMatchedIE;
import com.homa.ie.events.PayoutAccountSubmittedIE;
import com.homa.ie.events.PersonVerifiedIE;
import com.homa.verification.usecase.dto.RecordVerificationResultResponse;
import com.homa.verification.usecase.dto.StartVerificationRequest;
import com.homa.verification.usecase.dto.StartVerificationResponse;
import com.homa.verification.usecase.port.out.VerificationAnswer;
import com.homa.verification.usecase.port.out.VerificationSubmission;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** IDN-3: the identity document number is used only to recognise a person and to check payout accounts. */
class IdentityNumberNeverLeavesTest {

    private static final List<String> NUMBERS =
            List.of(AISYAH_MYKAD, AZMAN_MYKAD, SITI_MYKAD, RINA_IKAD, SON_MYKAD, DANIEL_MYKAD);

    private static void assertHoldsNoNumber(Object value) {
        String shown = String.valueOf(value);
        String compact = shown.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        for (String number : NUMBERS) {
            assertThat(shown).as("%s shows an identity number", value.getClass().getSimpleName()).doesNotContain(number);
            assertThat(compact).as("%s shows an identity number", value.getClass().getSimpleName())
                    .doesNotContain(number.replace("-", "").toUpperCase(Locale.ROOT));
        }
    }

    private static List<String> componentNames(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName)
                .map(name -> name.toLowerCase(Locale.ROOT)).toList();
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("Across every verification and payout check, no event and no response carries an identity number.")
    void noEventAndNoResponseCarriesTheNumber() {
        VerificationWorld world = new VerificationWorld();
        List<Object> responses = new ArrayList<>();
        StartVerificationResponse aisyah = world.start(AISYAH_SIGN_IN, CUSTOMER, HomaFixtures.AISYAH_CUSTOMER_ID, "MyKad");
        responses.add(aisyah);
        responses.add(world.answer(aisyah, aisyahsAnswer()));
        StartVerificationResponse azman = world.start(AZMAN_SIGN_IN, OWNER, null, "MyKad");
        responses.add(azman);
        responses.add(world.answer(azman, azmansAnswer()));
        responses.add(world.webhook("notification-1", azman));
        responses.add(world.verify(SITI_SIGN_IN, MEMBER, null, sitisAnswer()));
        responses.add(world.verify(RINA_SIGN_IN, MEMBER, null, rinasAnswer()));
        responses.add(world.verify(SON_SIGN_IN, CUSTOMER, SON_CUSTOMER_ID, sonsAnswer()));
        responses.add(world.verify(DANIEL_SIGN_IN, CUSTOMER, DANIEL_CUSTOMER_ID, danielsAnswer(at(2026, 11, 24, 18, 0))));
        RecordVerificationResultResponse second = world.verify(DANIEL_SECOND_SIGN_IN, CUSTOMER,
                DANIEL_SECOND_CUSTOMER_ID, danielsAnswer(at(2026, 11, 25, 9, 0)));
        responses.add(second);
        world.providerOwners.ownedBy(KEMAS_HOME_PROVIDER_ID, azman.personId());
        world.holderCheck.accountHeldBy("gateway-account-kemas-1", AZMAN_MYKAD);
        world.payoutAccountSubmitted.handle(new PayoutAccountSubmittedIE("payment-event-1", KEMAS_HOME_PROVIDER_ID,
                "payout-account-kemas-1", "gateway-account-kemas-1", "OWNER", at(2026, 10, 2, 11, 0)));

        assertThat(second.outcome()).isEqualTo("REFUSED");
        assertThat(world.publisher.published(PersonVerifiedIE.class)).hasSize(5);
        assertThat(world.publisher.published(PayoutAccountHolderMatchedIE.class)).hasSize(1);
        Stream.concat(world.publisher.published().stream(), responses.stream())
                .forEach(IdentityNumberNeverLeavesTest::assertHoldsNoNumber);
        assertThat(world.holderCheck.asked()).hasSize(1);
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("Verification's events and responses have no field that could hold an identity number.")
    void noEventOrResponseHasAFieldForTheNumber() {
        Stream.of(PersonVerifiedIE.class, PayoutAccountHolderMatchedIE.class, StartVerificationResponse.class,
                        RecordVerificationResultResponse.class)
                .flatMap(type -> componentNames(type).stream())
                .forEach(name -> assertThat(name).doesNotContain("number").doesNotContain("document"));
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("What carries the number into Homa, or out to the service, never shows it in a log.")
    void inboundDetailsWithholdTheNumber() {
        StartVerificationRequest request = new StartVerificationRequest(AISYAH_SIGN_IN, CUSTOMER,
                HomaFixtures.AISYAH_CUSTOMER_ID, "MyKad", AISYAH_MYKAD);
        VerificationSubmission submission = new VerificationSubmission("verification-1", "MyKad", AISYAH_MYKAD);
        VerificationAnswer.Passed answer = aisyahsAnswer();

        Stream.of(request, submission, answer).forEach(IdentityNumberNeverLeavesTest::assertHoldsNoNumber);
        assertThat(request).hasToString("StartVerificationRequest[signInId=sign-in-aisyah, role=CUSTOMER, "
                + "accountId=customer-aisyah, documentType=MyKad]");
        assertThat(submission).hasToString("VerificationSubmission[verificationId=verification-1, documentType=MyKad]");
        assertThat(answer.toString()).isEqualTo("Passed[status=LOCAL, documentType=MyKad, checkedAt="
                + VerificationWorld.AISYAH_CHECKED_AT + "]");
        assertThat(answer.toString()).doesNotContain(HomaFixtures.AISYAH_NAME);
        assertThat(request.documentNumber()).isEqualTo(AISYAH_MYKAD);
    }
}
