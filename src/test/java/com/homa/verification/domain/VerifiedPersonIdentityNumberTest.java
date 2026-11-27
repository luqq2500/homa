package com.homa.verification.domain;

import static com.homa.verification.domain.VerificationFixtures.DANIEL;
import static com.homa.verification.domain.VerificationFixtures.DANIELS_SECOND_ACCOUNT;
import static com.homa.verification.domain.VerificationFixtures.DANIEL_MYKAD;
import static com.homa.verification.domain.VerificationFixtures.HANA;
import static com.homa.verification.domain.VerificationFixtures.HANA_MYKAD;
import static com.homa.verification.domain.VerificationFixtures.MALAYSIA;
import static com.homa.verification.domain.VerificationFixtures.MYKAD;
import static com.homa.verification.domain.VerificationFixtures.SITI;
import static com.homa.verification.domain.VerificationFixtures.SITI_CHECKED_AT;
import static com.homa.verification.domain.VerificationFixtures.SITI_MYKAD;
import static com.homa.verification.domain.VerificationFixtures.danielsCheck;
import static com.homa.verification.domain.VerificationFixtures.rina;
import static com.homa.verification.domain.VerificationFixtures.siti;
import static com.homa.verification.domain.VerificationFixtures.sitisCheck;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.verification.domain.exception.IdentityNumberAlreadyHeldException;
import com.homa.verification.domain.exception.VerificationRuleViolationException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class VerifiedPersonIdentityNumberTest {

    @Test
    @Tag("CUS-10")
    @DisplayName("Daniel can't open a new account with another Google account, because his MyKad is already used.")
    void aSecondAccountCannotVerifyWithAnIdentityAlreadyUsed() {
        IdentityNumberStatus heldByDaniel = IdentityNumberStatus.heldBy(DANIEL);

        assertThatThrownBy(() -> VerifiedPerson.verify(DANIELS_SECOND_ACCOUNT, PersonRole.CUSTOMER, danielsCheck(),
                MALAYSIA, heldByDaniel))
                .isInstanceOf(IdentityNumberAlreadyHeldException.class)
                .hasMessage("IDN-3: this identity document is already verified for another person")
                .hasMessageNotContaining(DANIEL_MYKAD)
                .hasMessageNotContaining(new IdentityNumber(DANIEL_MYKAD).value())
                .hasMessageNotContaining(DANIEL.value())
                .extracting(exception -> ((VerificationRuleViolationException) exception).ruleId()).isEqualTo("IDN-3");
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Siti, a Kemas Home helper, can't be verified again as someone else to open her own provider.")
    void aVerifiedMemberCannotBeVerifiedUnderAnotherPersonId() {
        assertThatThrownBy(() -> VerifiedPerson.verify(new PersonId("person-siti-again"), PersonRole.OWNER,
                sitisCheck(), MALAYSIA, IdentityNumberStatus.heldBy(SITI)))
                .isInstanceOf(IdentityNumberAlreadyHeldException.class);
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("Siti's own MyKad number matches her when she verifies again, so she stays one person.")
    void thePersonWhoHoldsTheNumberIsAdmittedAgain() {
        VerifiedPerson again = VerifiedPerson.verify(SITI, PersonRole.MEMBER, sitisCheck(), MALAYSIA,
                IdentityNumberStatus.heldBy(SITI));

        assertThat(again.id()).isEqualTo(SITI);
        assertThat(IdentityNumberStatus.heldBy(SITI).holder()).contains(SITI);
        assertThat(IdentityNumberStatus.free().holder()).isEmpty();
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("When Hana tries to open Hana Cleaning again, her MyKad number matches, however it is written.")
    void theNumberRecognisesThePersonWhateverItsFormatting() {
        IdentityNumber typedWithHyphens = new IdentityNumber(HANA_MYKAD);
        IdentityNumber typedPlain = new IdentityNumber(" 850923 086234 ");

        assertThat(typedWithHyphens).isEqualTo(typedPlain);
        assertThat(typedWithHyphens.value()).isEqualTo("850923086234");
        assertThat(new IdentityNumber("pw2611-0451")).isEqualTo(new IdentityNumber("PW26110451"));
        assertThat(HANA.value()).isEqualTo("person-hana");
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("Customers and providers never see an identity number: it never appears in a printed record.")
    void theIdentityNumberNeverAppearsInToString() {
        VerifiedPerson siti = siti();
        IdentityDocument document = new IdentityDocument(MYKAD, new IdentityNumber(SITI_MYKAD));
        String plainNumber = new IdentityNumber(SITI_MYKAD).value();

        assertThat(siti).hasToString("VerifiedPerson[id=person-siti, status=LOCAL, document=MyKad, result="
                + new VerificationResult(VerificationOutcome.PASSED, SITI_CHECKED_AT, true) + "]");
        assertThat(new IdentityNumber(SITI_MYKAD)).hasToString("IdentityNumber[withheld]");
        assertThat(Stream.of(siti.toString(), document.toString(), sitisCheck().toString(),
                rina().toString()))
                .allSatisfy(text -> assertThat(text).doesNotContain(SITI_MYKAD).doesNotContain(plainNumber)
                        .doesNotContain("PW2611"));
    }
}
