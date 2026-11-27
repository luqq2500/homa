package com.homa.verification.domain;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.verification.domain.VerificationFixtures.ADULT_BORN;
import static com.homa.verification.domain.VerificationFixtures.AISYAH;
import static com.homa.verification.domain.VerificationFixtures.AISYAHS_SON;
import static com.homa.verification.domain.VerificationFixtures.AISYAHS_SON_BORN;
import static com.homa.verification.domain.VerificationFixtures.AISYAHS_SON_MYKAD;
import static com.homa.verification.domain.VerificationFixtures.AISYAH_CHECKED_AT;
import static com.homa.verification.domain.VerificationFixtures.AZMAN;
import static com.homa.verification.domain.VerificationFixtures.FARID;
import static com.homa.verification.domain.VerificationFixtures.FARID_MYKAD;
import static com.homa.verification.domain.VerificationFixtures.HANA;
import static com.homa.verification.domain.VerificationFixtures.HANA_MYKAD;
import static com.homa.verification.domain.VerificationFixtures.MALAYSIA;
import static com.homa.verification.domain.VerificationFixtures.MEMBER_CHECKED_AT;
import static com.homa.verification.domain.VerificationFixtures.RINA;
import static com.homa.verification.domain.VerificationFixtures.RINA_IKAD_EXPIRY;
import static com.homa.verification.domain.VerificationFixtures.aisyah;
import static com.homa.verification.domain.VerificationFixtures.azman;
import static com.homa.verification.domain.VerificationFixtures.localMyKadCheck;
import static com.homa.verification.domain.VerificationFixtures.rina;
import static com.homa.verification.domain.VerificationFixtures.rinasCheck;
import static com.homa.verification.domain.VerificationFixtures.siti;
import static com.homa.verification.domain.VerificationFixtures.verified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.verification.domain.exception.PersonUnderageException;
import com.homa.verification.domain.exception.StatusNotAllowedForRoleException;
import com.homa.verification.domain.exception.VerificationRuleViolationException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class VerifiedPersonEligibilityTest {

    @Test
    @Tag("CUS-10")
    @DisplayName("Aisyah verifies with her MyKad, so she can book.")
    void aLocalAdultIsVerifiedAsACustomer() {
        VerifiedPerson aisyah = aisyah();

        assertThat(aisyah.id()).isEqualTo(AISYAH);
        assertThat(aisyah.result().ageCheckPassed()).isTrue();
        assertThatCode(() -> aisyah.requireEligibleAs(PersonRole.CUSTOMER, MALAYSIA)).doesNotThrowAnyException();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Aisyah's 16-year-old son can't book.")
    void aSixteenYearOldCannotBeVerifiedAsACustomer() {
        IdentityCheck son = localMyKadCheck("Aisyah's son", AISYAHS_SON_MYKAD, AISYAHS_SON_BORN, AISYAH_CHECKED_AT);

        assertThatThrownBy(() -> verified(AISYAHS_SON, PersonRole.CUSTOMER, son))
                .isInstanceOf(PersonUnderageException.class)
                .hasMessage("CUS-10: a customer must be aged 18 or above")
                .extracting(exception -> ((VerificationRuleViolationException) exception).ruleId())
                .isEqualTo("CUS-10");
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Rina, who is foreign, can't be a customer in Malaysia.")
    void aForeignPersonCannotBeVerifiedAsACustomer() {
        assertThatThrownBy(() -> verified(RINA, PersonRole.CUSTOMER, rinasCheck()))
                .isInstanceOf(StatusNotAllowedForRoleException.class)
                .hasMessage("CUS-10: in Malaysia a customer can't be foreign");
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Siti, already verified as a Kemas Home helper, can also book as a customer.")
    void aVerifiedMemberCanAlsoBeACustomer() {
        assertThatCode(() -> siti().requireEligibleAs(PersonRole.CUSTOMER, MALAYSIA)).doesNotThrowAnyException();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Hana verifies with her MyKad and opens Hana Cleaning. Azman, also verified, opens Kemas Home.")
    void localAdultsAreVerifiedAsOwners() {
        VerifiedPerson hana = verified(HANA, PersonRole.OWNER,
                localMyKadCheck("Hana", HANA_MYKAD, LocalDate.of(1985, 9, 23), at(2026, 9, 1, 9, 0)));

        assertThat(hana.id()).isEqualTo(HANA);
        assertThat(azman().id()).isEqualTo(AZMAN);
        assertThatCode(() -> azman().requireEligibleAs(PersonRole.OWNER, MALAYSIA)).doesNotThrowAnyException();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Rina, verified as a Kemas Home helper, can't open her own provider: an owner must be local.")
    void aForeignPersonCannotBeAnOwner() {
        VerifiedPerson rina = rina();

        assertThatThrownBy(() -> rina.requireEligibleAs(PersonRole.OWNER, MALAYSIA))
                .isInstanceOf(StatusNotAllowedForRoleException.class)
                .hasMessage("PRV-1: in Malaysia an owner can't be foreign");
        assertThatThrownBy(() -> verified(RINA, PersonRole.OWNER, rinasCheck()))
                .isInstanceOf(StatusNotAllowedForRoleException.class);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A 17-year-old can't open a provider.")
    void anUnderageOwnerIsRefused() {
        IdentityCheck seventeen = localMyKadCheck("Trainee", "090105-10-5011", LocalDate.of(2009, 1, 5),
                MEMBER_CHECKED_AT);

        assertThatThrownBy(() -> verified(new PersonId("person-trainee"), PersonRole.OWNER, seventeen))
                .isInstanceOf(PersonUnderageException.class)
                .hasMessage("PRV-1: an owner must be aged 18 or above");
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("Kemas Home can't add a 17-year-old trainee. A helper who turned 18 last week can be added.")
    void aMemberMustBeAged18OrAbove() {
        IdentityCheck trainee = localMyKadCheck("Trainee", "090105-10-5011", LocalDate.of(2009, 1, 5),
                MEMBER_CHECKED_AT);
        IdentityCheck turned18LastWeek = localMyKadCheck("New helper", "081026-10-5022", LocalDate.of(2008, 10, 26),
                MEMBER_CHECKED_AT);

        assertThatThrownBy(() -> verified(new PersonId("person-trainee"), PersonRole.MEMBER, trainee))
                .isInstanceOf(PersonUnderageException.class)
                .hasMessage("HLP-1: a member must be aged 18 or above");
        assertThat(verified(new PersonId("person-new-helper"), PersonRole.MEMBER, turned18LastWeek)
                .result().ageCheckPassed()).isTrue();
    }

    @Test
    @Tag("HLP-2")
    @DisplayName("Siti verifies with her MyKad as a local member; so does supervisor Farid.")
    void aLocalMemberIsVerifiedAsLocal() {
        VerifiedPerson farid = verified(FARID, PersonRole.MEMBER,
                localMyKadCheck("Farid", FARID_MYKAD, ADULT_BORN, MEMBER_CHECKED_AT));

        assertThat(siti().status()).isEqualTo(PersonStatus.LOCAL);
        assertThat(farid.status()).isEqualTo(PersonStatus.LOCAL);
        assertThat(farid.permitExpiry()).isEmpty();
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("Rina is verified as a foreign member with her permit's expiry, for Kemas Home to check its 90 days.")
    void aForeignMemberIsVerifiedWithTheirPermitExpiry() {
        VerifiedPerson rina = rina();

        assertThat(rina.status()).isEqualTo(PersonStatus.FOREIGN);
        assertThat(rina.permitExpiry()).contains(RINA_IKAD_EXPIRY);
        assertThatCode(() -> rina.requireEligibleAs(PersonRole.MEMBER, MALAYSIA)).doesNotThrowAnyException();
    }
}
