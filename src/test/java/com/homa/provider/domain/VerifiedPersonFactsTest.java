package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.RINA_PERMIT_EXPIRY;
import static com.homa.provider.domain.ProviderFixtures.RINA_PERSON;
import static com.homa.provider.domain.ProviderFixtures.SITI_PERSON;
import static com.homa.provider.domain.ProviderFixtures.rinaVerified;
import static com.homa.provider.domain.ProviderFixtures.sitiVerified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class VerifiedPersonFactsTest {

    @Test
    @Tag("IDN-1")
    @DisplayName("Siti verifies with her MyKad and Rina with her i-Kad, whose date is used as her permit's expiry.")
    void verificationSaysWhetherAPersonIsLocalOrForeign() {
        VerifiedPersonFacts siti = sitiVerified();
        VerifiedPersonFacts rina = rinaVerified();

        assertThat(siti.personId()).isEqualTo(SITI_PERSON);
        assertThat(siti.local()).isTrue();
        assertThat(siti.ageCheckPassed()).isTrue();
        assertThat(siti.permitExpiry()).isNull();
        assertThat(rina.personId()).isEqualTo(RINA_PERSON);
        assertThat(rina.local()).isFalse();
        assertThat(rina.ageCheckPassed()).isTrue();
        assertThat(rina.permitExpiry()).isEqualTo(RINA_PERMIT_EXPIRY);
        assertThat(VerifiedPersonFacts.local(SITI_PERSON, false).ageCheckPassed()).isFalse();
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("A local person has no work permit, and a foreign candidate without an i-Kad can't be verified.")
    void localPersonHasNoPermitAndForeignPersonAlwaysHasOne() {
        assertThatThrownBy(() -> new VerifiedPersonFacts(SITI_PERSON, true, true, RINA_PERMIT_EXPIRY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VerifiedPersonFacts.foreign(RINA_PERSON, true, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VerifiedPersonFacts.local(null, true)).isInstanceOf(NullPointerException.class);
    }
}
