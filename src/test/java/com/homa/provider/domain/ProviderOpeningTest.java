package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.AZMAN;
import static com.homa.provider.domain.ProviderFixtures.HANA;
import static com.homa.provider.domain.ProviderFixtures.HANA_CLEANING;
import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.RINA_PERMIT_EXPIRY;
import static com.homa.provider.domain.ProviderFixtures.RINA_PERSON;
import static com.homa.provider.domain.ProviderFixtures.openedHanaCleaning;
import static com.homa.provider.domain.ProviderFixtures.openedKemasHome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.provider.domain.exception.OwnerNotLocalException;
import com.homa.provider.domain.exception.OwnerUnderageException;
import com.homa.provider.domain.exception.ProviderRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderOpeningTest {

    private static final PersonId DANIEL = new PersonId("person-daniel");

    @Test
    @Tag("PRV-1")
    @DisplayName("Hana verifies with her MyKad and opens Hana Cleaning. Azman, also verified, opens Kemas Home.")
    void verifiedLocalAdultOpensAProviderAsItsOwner() {
        Provider hanaCleaning = openedHanaCleaning();
        Provider kemasHome = openedKemasHome();

        assertThat(hanaCleaning.id()).isEqualTo(HANA_CLEANING);
        assertThat(hanaCleaning.owner()).isEqualTo(new Owner(HANA));
        assertThat(kemasHome.id()).isEqualTo(KEMAS_HOME);
        assertThat(kemasHome.owner().personId()).isEqualTo(AZMAN);
        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.OPENED);
        assertThat(kemasHome.isLive()).isFalse();
        assertThat(kemasHome.verifiedPayoutAccount()).isEmpty();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A person verified as foreign can't own a provider: Malaysia's country policy allows only locals as owners.")
    void personVerifiedAsForeignCannotOpenAProvider() {
        VerifiedPersonFacts rina = VerifiedPersonFacts.foreign(RINA_PERSON, true, RINA_PERMIT_EXPIRY);

        assertThatThrownBy(() -> Provider.open(new ProviderId("provider-rina"), rina))
                .isInstanceOf(OwnerNotLocalException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(RINA_PERSON.value());
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A person who doesn't pass the age check can't own a provider: every owner is aged 18 or above.")
    void personUnder18CannotOpenAProvider() {
        VerifiedPersonFacts underage = VerifiedPersonFacts.local(DANIEL, false);

        assertThatThrownBy(() -> Provider.open(new ProviderId("provider-daniel"), underage))
                .isInstanceOf(OwnerUnderageException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(DANIEL.value());
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Every provider has one owner, a person.")
    void everyProviderHasAnOwner() {
        assertThatThrownBy(() -> new Owner(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Provider.restore(KEMAS_HOME, null, ProviderStatus.OPENED, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A stored provider is rebuilt as it was saved, with its owner.")
    void restoredProviderKeepsItsStoredState() {
        Provider kemasHome = Provider.restore(KEMAS_HOME, new Owner(AZMAN), ProviderStatus.LIVE,
                PayoutAccountHolder.BUSINESS);
        Provider hanaCleaning = Provider.restore(HANA_CLEANING, new Owner(HANA), ProviderStatus.OPENED, null);

        assertThat(kemasHome.id()).isEqualTo(KEMAS_HOME);
        assertThat(kemasHome.owner()).isEqualTo(new Owner(AZMAN));
        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.LIVE);
        assertThat(kemasHome.isLive()).isTrue();
        assertThat(kemasHome.verifiedPayoutAccount()).contains(PayoutAccountHolder.BUSINESS);
        assertThat(hanaCleaning.isLive()).isFalse();
        assertThat(hanaCleaning.verifiedPayoutAccount()).isEmpty();
    }
}
