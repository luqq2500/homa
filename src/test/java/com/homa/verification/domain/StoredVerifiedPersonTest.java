package com.homa.verification.domain;

import static com.homa.verification.domain.VerificationFixtures.AISYAH;
import static com.homa.verification.domain.VerificationFixtures.MALAYSIA;
import static com.homa.verification.domain.VerificationFixtures.RINA_IKAD_EXPIRY;
import static com.homa.verification.domain.VerificationFixtures.aisyah;
import static com.homa.verification.domain.VerificationFixtures.rina;
import static com.homa.verification.domain.VerificationFixtures.siti;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.verification.domain.exception.StatusNotAllowedForRoleException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredVerifiedPersonTest {

    private static VerifiedPerson restored(VerifiedPerson stored) {
        return storedAs(stored, stored.status(), stored.result(), stored.permitExpiry().orElse(null));
    }

    private static VerifiedPerson storedAs(VerifiedPerson from, PersonStatus status, VerificationResult result,
            LocalDate permitExpiry) {
        return VerifiedPerson.restore(from.id(), from.name(), status,
                new IdentityDocument(from.identityDocumentType(), from.identityNumber()), result, permitExpiry);
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("Persistence: Aisyah comes back with only what IDN-2 keeps, still eligible as a customer and never re-verifying.")
    void aStoredLocalPersonComesBackAsVerified() {
        VerifiedPerson aisyah = aisyah();

        VerifiedPerson stored = restored(aisyah);

        assertThat(stored).usingRecursiveComparison().isEqualTo(aisyah);
        assertThat(stored.toString()).isEqualTo(aisyah.toString());
        stored.requireEligibleAs(PersonRole.CUSTOMER, MALAYSIA);
        assertThat(stored.nextDueAt(PersonRole.CUSTOMER)).isEmpty();
        assertThat(stored.nextDueAt(PersonRole.OWNER)).isEqualTo(aisyah.nextDueAt(PersonRole.OWNER));
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("Persistence: Rina comes back with her i-Kad's permit expiry, eligible as a member but not as a customer.")
    void aStoredForeignPersonKeepsHerPermitExpiry() {
        VerifiedPerson rina = rina();

        VerifiedPerson stored = restored(rina);

        assertThat(stored).usingRecursiveComparison().isEqualTo(rina);
        assertThat(stored.permitExpiry()).contains(RINA_IKAD_EXPIRY);
        stored.requireEligibleAs(PersonRole.MEMBER, MALAYSIA);
        assertThatThrownBy(() -> stored.requireEligibleAs(PersonRole.CUSTOMER, MALAYSIA))
                .isInstanceOf(StatusNotAllowedForRoleException.class);
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("Persistence: a stored verified person needs a passed result, and a permit expiry exactly when foreign.")
    void aStoredPersonThatDoesNotFitTogetherIsRefused() {
        VerifiedPerson siti = siti();
        VerificationResult failed = new VerificationResult(VerificationOutcome.FAILED, siti.result().checkedAt(), true);

        assertThatThrownBy(() -> storedAs(siti, PersonStatus.LOCAL, failed, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("A stored verified person needs ");
        assertThatThrownBy(() -> storedAs(siti, PersonStatus.LOCAL, siti.result(), RINA_IKAD_EXPIRY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storedAs(siti, PersonStatus.FOREIGN, siti.result(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(storedAs(siti, PersonStatus.FOREIGN, siti.result(), RINA_IKAD_EXPIRY).status())
                .isEqualTo(PersonStatus.FOREIGN);
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("Persistence: a stored verified person needs its id, name, status, document and result.")
    void aStoredPersonMissingAPartIsRefused() {
        VerifiedPerson siti = siti();
        IdentityDocument document = new IdentityDocument(siti.identityDocumentType(), siti.identityNumber());

        assertThatThrownBy(() -> VerifiedPerson.restore(null, siti.name(), PersonStatus.LOCAL, document,
                siti.result(), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> VerifiedPerson.restore(AISYAH, null, PersonStatus.LOCAL, document, siti.result(),
                null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> VerifiedPerson.restore(AISYAH, siti.name(), null, document, siti.result(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> VerifiedPerson.restore(AISYAH, siti.name(), PersonStatus.LOCAL, null,
                siti.result(), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> VerifiedPerson.restore(AISYAH, siti.name(), PersonStatus.LOCAL, document, null,
                null)).isInstanceOf(NullPointerException.class);
    }
}
