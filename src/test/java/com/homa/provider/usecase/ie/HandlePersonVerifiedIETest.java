package com.homa.provider.usecase.ie;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.AZMAN;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NEXT_DUE_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA_PERMIT_EXPIRY;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.azmanVerified;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.localMemberVerified;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.personVerified;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.rinaVerified;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.PersonVerifiedIE;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.VerifiedPersonFacts;
import com.homa.provider.usecase.ProviderWorld;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandlePersonVerifiedIETest {

    private final ProviderWorld world = new ProviderWorld();

    private VerifiedPerson kept(String personId) {
        return world.verifiedPeople.findByPersonId(new PersonId(personId)).orElseThrow();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Azman verifies with his MyKad as an owner: Provider keeps him as local, aged 18 or above, named Azman.")
    void ownersVerificationIsKept() {
        world.personVerified.handle(azmanVerified("verification-1"));

        VerifiedPerson azman = kept(AZMAN);
        assertThat(azman.facts()).isEqualTo(VerifiedPersonFacts.local(new PersonId(AZMAN), true, NEXT_DUE_AT));
        assertThat(azman.name()).isEqualTo("Azman");
        assertThat(azman.personId()).isEqualTo(new PersonId(AZMAN));
        assertThat(world.processedMessages.hasHandled("verification-1", "provider.PersonVerified")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-3")
    @Tag("IDN-1")
    @DisplayName("Rina verifies with her i-Kad as a member: Provider keeps her as foreign, with her permit's expiry.")
    void foreignMembersVerificationKeepsThePermit() {
        world.personVerified.handle(rinaVerified("verification-2"));

        VerifiedPerson rina = kept(RINA);
        assertThat(rina.facts()).isEqualTo(VerifiedPersonFacts.foreign(new PersonId(RINA), true, RINA_PERMIT_EXPIRY, NEXT_DUE_AT));
        assertThat(rina.name()).isEqualTo("Rina");
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("A trainee who didn't pass the age check is kept as such, so adding her is refused later.")
    void failedAgeCheckIsKept() {
        world.personVerified.handle(
                personVerified("verification-3", "person-trainee", "MEMBER", "Trainee", "LOCAL", false, null));

        assertThat(kept("person-trainee").facts().ageCheckPassed()).isFalse();
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("A verification started for a customer account is Customer's, so Provider keeps nothing.")
    void customerVerificationIsIgnored() {
        PersonVerifiedIE aisyah = new PersonVerifiedIE("verification-4", "person-aisyah", "CUSTOMER",
                "customer-aisyah", "Aisyah", "LOCAL", true, null, null, Instant.EPOCH);

        world.personVerified.handle(aisyah);

        assertThat(world.verifiedPeople.count()).isZero();
        assertThat(world.processedMessages.hasHandled("verification-4", "provider.PersonVerified")).isTrue();
    }

    @Test
    @Tag("MON-1")
    @DisplayName("Siti re-verifies: what Verification decided last replaces what was kept.")
    void reverificationReplacesTheEarlierFacts() {
        world.personVerified.handle(localMemberVerified("verification-5", SITI, "Siti"));
        world.personVerified.handle(localMemberVerified("verification-6", SITI, "Siti Aminah"));

        assertThat(kept(SITI).name()).isEqualTo("Siti Aminah");
        assertThat(world.verifiedPeople.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("A redelivered PersonVerifiedIE changes nothing and publishes nothing.")
    void redeliveryChangesNothing() {
        world.personVerified.handle(localMemberVerified("verification-7", SITI, "Siti"));
        world.personVerified.handle(localMemberVerified("verification-8", SITI, "Siti Aminah"));

        world.personVerified.handle(localMemberVerified("verification-7", SITI, "Siti"));

        assertThat(kept(SITI).name()).isEqualTo("Siti Aminah");
        assertThat(world.publisher.published()).isEmpty();
    }
}
