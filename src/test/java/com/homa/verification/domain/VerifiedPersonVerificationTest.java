package com.homa.verification.domain;

import static com.homa.verification.domain.VerificationFixtures.ADULT_BORN;
import static com.homa.verification.domain.VerificationFixtures.AISYAH;
import static com.homa.verification.domain.VerificationFixtures.AISYAH_CHECKED_AT;
import static com.homa.verification.domain.VerificationFixtures.AISYAH_MYKAD;
import static com.homa.verification.domain.VerificationFixtures.IKAD;
import static com.homa.verification.domain.VerificationFixtures.MEMBER_CHECKED_AT;
import static com.homa.verification.domain.VerificationFixtures.MYKAD;
import static com.homa.verification.domain.VerificationFixtures.MYPR;
import static com.homa.verification.domain.VerificationFixtures.NURUL;
import static com.homa.verification.domain.VerificationFixtures.NURUL_MYPR;
import static com.homa.verification.domain.VerificationFixtures.PASSPORT;
import static com.homa.verification.domain.VerificationFixtures.RINA;
import static com.homa.verification.domain.VerificationFixtures.RINA_CHECKED_AT;
import static com.homa.verification.domain.VerificationFixtures.RINA_IKAD;
import static com.homa.verification.domain.VerificationFixtures.RINA_IKAD_EXPIRY;
import static com.homa.verification.domain.VerificationFixtures.SITI;
import static com.homa.verification.domain.VerificationFixtures.SITI_CHECKED_AT;
import static com.homa.verification.domain.VerificationFixtures.SITI_MYKAD;
import static com.homa.verification.domain.VerificationFixtures.aisyah;
import static com.homa.verification.domain.VerificationFixtures.aisyahsCheck;
import static com.homa.verification.domain.VerificationFixtures.passedCheck;
import static com.homa.verification.domain.VerificationFixtures.rina;
import static com.homa.verification.domain.VerificationFixtures.siti;
import static com.homa.verification.domain.VerificationFixtures.verified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.verification.domain.exception.DocumentNotSetForStatusException;
import com.homa.verification.domain.exception.IdentityNotVerifiedException;
import com.homa.verification.domain.exception.MissingIdentityDetailException;
import com.homa.verification.domain.exception.WorkPermitExpiryMissingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class VerifiedPersonVerificationTest {

    private static List<Field> instanceFields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !field.isSynthetic() && !Modifier.isStatic(field.getModifiers()))
                .toList();
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("Aisyah and Siti verify with their MyKad, and Rina with her i-Kad.")
    void eachPersonVerifiesWithTheDocumentThePolicySetsForTheirStatus() {
        assertThat(aisyah().identityDocumentType()).isEqualTo(MYKAD);
        assertThat(aisyah().status()).isEqualTo(PersonStatus.LOCAL);
        assertThat(siti().identityDocumentType()).isEqualTo(MYKAD);
        assertThat(rina().identityDocumentType()).isEqualTo(IKAD);
        assertThat(rina().status()).isEqualTo(PersonStatus.FOREIGN);
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("Nurul, a permanent resident, verifies as local with her MyPR.")
    void aPermanentResidentVerifiesAsLocalWithTheirMyPR() {
        VerifiedPerson nurul = verified(NURUL, PersonRole.MEMBER,
                passedCheck("Nurul", PersonStatus.LOCAL, MYPR, NURUL_MYPR, ADULT_BORN, null, MEMBER_CHECKED_AT));

        assertThat(nurul.status()).isEqualTo(PersonStatus.LOCAL);
        assertThat(nurul.identityDocumentType()).isEqualTo(MYPR);
        assertThat(nurul.permitExpiry()).isEmpty();
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("A foreign candidate without an i-Kad can't be added: a passport doesn't verify them.")
    void aForeignCandidateWithoutAnIKadCannotBeVerified() {
        IdentityCheck withPassport = passedCheck("Candidate", PersonStatus.FOREIGN, PASSPORT, "A12345678",
                ADULT_BORN, RINA_IKAD_EXPIRY, MEMBER_CHECKED_AT);

        assertThatThrownBy(() -> verified(new PersonId("person-candidate"), PersonRole.MEMBER, withPassport))
                .isInstanceOf(DocumentNotSetForStatusException.class)
                .hasMessage("IDN-1: in Malaysia a foreign person isn't verified with Passport");
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("Rina's i-Kad can't verify her as local: the document must be the one set for her status.")
    void aDocumentForAnotherStatusCannotVerifyThePerson() {
        IdentityCheck ikadAsLocal = passedCheck("Rina", PersonStatus.LOCAL, IKAD, RINA_IKAD, ADULT_BORN,
                RINA_IKAD_EXPIRY, RINA_CHECKED_AT);

        assertThatThrownBy(() -> verified(RINA, PersonRole.MEMBER, ikadAsLocal))
                .isInstanceOf(DocumentNotSetForStatusException.class)
                .hasMessage("IDN-1: in Malaysia a local person isn't verified with i-Kad");
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("When the verification service doesn't pass Aisyah's MyKad, she isn't verified.")
    void aFailedCheckVerifiesNobody() {
        IdentityCheck passed = aisyahsCheck();
        IdentityCheck failed = new IdentityCheck(passed.name(), passed.status(), passed.document(),
                passed.dateOfBirth(), null, VerificationOutcome.FAILED, passed.checkedAt());

        assertThatThrownBy(() -> verified(AISYAH, PersonRole.CUSTOMER, failed))
                .isInstanceOf(IdentityNotVerifiedException.class)
                .hasMessage("IDN-1: the identity document did not pass verification, so the person can't be verified")
                .extracting(exception -> ((IdentityNotVerifiedException) exception).ruleId()).isEqualTo("IDN-1");
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("Homa holds Siti's name, \"local\", \"passed, 20 November\" and her MyKad number.")
    void sitisRecordHoldsHerNameStatusResultAndMyKadNumber() {
        VerifiedPerson siti = siti();

        assertThat(siti.id()).isEqualTo(SITI);
        assertThat(siti.name()).isEqualTo(new PersonName("Siti"));
        assertThat(siti.status()).isEqualTo(PersonStatus.LOCAL);
        assertThat(siti.result()).isEqualTo(new VerificationResult(VerificationOutcome.PASSED, SITI_CHECKED_AT, true));
        assertThat(siti.identityDocumentType()).isEqualTo(MYKAD);
        assertThat(siti.identityNumber()).isEqualTo(new IdentityNumber(SITI_MYKAD));
        assertThat(siti.permitExpiry()).isEmpty();
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("Homa doesn't store a picture of Siti's card or her face scan: no field could hold one.")
    void aVerifiedPersonHoldsOnlyTheFieldsIdentityRulesList() {
        assertThat(instanceFields(VerifiedPerson.class)).extracting(Field::getName).containsExactlyInAnyOrder(
                "id", "name", "status", "identityDocument", "result", "permitExpiry");
        Stream.of(VerifiedPerson.class, IdentityCheck.class, IdentityDocument.class, IdentityNumber.class,
                VerificationResult.class, PersonName.class).forEach(type ->
                assertThat(instanceFields(type)).as("fields of %s", type.getSimpleName())
                        .noneMatch(field -> field.getType().isArray()));
        assertThat(Arrays.stream(IdentityCheck.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("name", "status", "document", "dateOfBirth", "documentExpiry", "outcome", "checkedAt");
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("Aisyah's date of birth decides her age check, and only the result is kept.")
    void theDateOfBirthDecidesTheAgeCheckAndIsNotKept() {
        VerifiedPerson aisyah = aisyah();

        assertThat(aisyah.result().ageCheckPassed()).isTrue();
        assertThat(aisyah.result().checkedAt()).isEqualTo(AISYAH_CHECKED_AT);
        assertThat(instanceFields(VerifiedPerson.class)).noneMatch(field -> field.getType() == LocalDate.class
                && !field.getName().equals("permitExpiry"));
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("A local's document date isn't kept: Siti has no permit expiry.")
    void aLocalsDocumentExpiryIsNotKept() {
        VerifiedPerson siti = verified(SITI, PersonRole.MEMBER, passedCheck("Siti", PersonStatus.LOCAL, MYKAD,
                SITI_MYKAD, ADULT_BORN, LocalDate.of(2031, 1, 1), SITI_CHECKED_AT));

        assertThat(siti.permitExpiry()).isEmpty();
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("A verified person must have a name and an identity document number.")
    void aVerifiedPersonMustHaveANameAndANumber() {
        assertThatThrownBy(() -> new PersonName(null)).isInstanceOf(MissingIdentityDetailException.class)
                .hasMessage("IDN-2: a verified person must have a name");
        assertThatThrownBy(() -> new PersonName("  ")).isInstanceOf(MissingIdentityDetailException.class);
        assertThatThrownBy(() -> new IdentityNumber(null)).isInstanceOf(MissingIdentityDetailException.class)
                .hasMessage("IDN-2: a verified person must have an identity document number");
        assertThatThrownBy(() -> new IdentityNumber(" - ")).isInstanceOf(MissingIdentityDetailException.class)
                .hasMessage("IDN-2: a verified person must have an identity document number");
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("Rina's i-Kad date is used as her work permit's expiry: 200 days after she is verified.")
    void rinasIKadDateIsHerPermitExpiry() {
        VerifiedPerson rina = rina();

        assertThat(rina.permitExpiry()).contains(RINA_IKAD_EXPIRY);
        assertThat(RINA_IKAD_EXPIRY).isEqualTo(LocalDate.of(2027, 5, 21));
        assertThat(LocalDate.of(2026, 11, 2).until(RINA_IKAD_EXPIRY, ChronoUnit.DAYS)).isEqualTo(200);
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("A foreign candidate whose i-Kad gives no date can't be verified without a permit expiry.")
    void aForeignPersonNeedsAPermitExpiry() {
        IdentityCheck undated = passedCheck("Rina", PersonStatus.FOREIGN, IKAD, RINA_IKAD, ADULT_BORN, null,
                RINA_CHECKED_AT);

        assertThatThrownBy(() -> verified(RINA, PersonRole.MEMBER, undated))
                .isInstanceOf(WorkPermitExpiryMissingException.class)
                .hasMessage("HLP-3: a foreign person needs a work permit expiry, and their i-Kad doesn't give one");
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("Where a country's foreign document doesn't date the permit, it gives no permit expiry.")
    void aForeignDocumentThatDoesNotDateThePermitGivesNoExpiry() {
        IdentityDocumentType visa = new IdentityDocumentType("Visit visa");
        CountryPolicy elsewhere = new CountryPolicy("Elsewhere",
                List.of(new AcceptedDocument(visa, PersonStatus.FOREIGN, "visitors", false)),
                Set.of(PersonStatus.LOCAL), Set.of(PersonStatus.LOCAL), new BusinessRegistry("Registry", "R"),
                List.of(new NoticeLanguage("en", "English")), "Data Act", Period.ofDays(30));
        IdentityCheck dated = passedCheck("Rina", PersonStatus.FOREIGN, visa, RINA_IKAD, ADULT_BORN,
                RINA_IKAD_EXPIRY, RINA_CHECKED_AT);

        assertThatThrownBy(() -> VerifiedPerson.verify(RINA, PersonRole.MEMBER, dated, elsewhere,
                IdentityNumberStatus.free()))
                .isInstanceOf(WorkPermitExpiryMissingException.class)
                .hasMessage("HLP-3: a foreign person needs a work permit expiry, and their Visit visa doesn't give one");
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("Aisyah's MyKad check names her as she is on her card.")
    void theVerifiedPersonCarriesTheNameFromTheirDocument() {
        assertThat(aisyah().name().value()).isEqualTo("Aisyah");
        assertThat(aisyah().identityNumber().value()).isEqualTo(AISYAH_MYKAD.replace("-", ""));
    }
}
