package com.homa.provider.usecase;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.AZMAN;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.HANA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA_PERMIT_EXPIRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.provider.domain.Owner;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.domain.VerifiedPersonFacts;
import com.homa.provider.domain.exception.OwnerNotLocalException;
import com.homa.provider.domain.exception.OwnerUnderageException;
import com.homa.provider.usecase.dto.OpenProviderRequest;
import com.homa.provider.usecase.dto.OpenProviderResponse;
import com.homa.provider.usecase.exception.PersonNotVerifiedException;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class OwnerOpenProviderTest {

    private final ProviderWorld world = new ProviderWorld();

    @Test
    @Tag("PRV-1")
    @DisplayName("Azman, verified with his MyKad, opens Kemas Home as its owner; it isn't live and has no payout account.")
    void verifiedLocalAdultOpensAProvider() {
        world.verifiedPeople.save(ProviderUsecaseFixtures.verifiedLocal(AZMAN, "Azman"));

        OpenProviderResponse response = world.openProvider.open(new OpenProviderRequest(AZMAN));

        assertThat(response.providerId()).isNotBlank();
        assertThat(response.ownerPersonId()).isEqualTo(AZMAN);
        assertThat(response.status()).isEqualTo("OPENED");
        Provider stored = world.providers.stored(new ProviderId(response.providerId()));
        assertThat(stored.owner()).isEqualTo(new Owner(new PersonId(AZMAN)));
        assertThat(stored.status()).isEqualTo(ProviderStatus.OPENED);
        assertThat(stored.verifiedPayoutAccount()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Hana and Azman each open their own provider, under different ids.")
    void eachOpeningIsANewProvider() {
        world.verifiedPeople.save(ProviderUsecaseFixtures.verifiedLocal(AZMAN, "Azman"));
        world.verifiedPeople.save(ProviderUsecaseFixtures.verifiedLocal(HANA, "Hana"));

        OpenProviderResponse kemasHome = world.openProvider.open(new OpenProviderRequest(AZMAN));
        OpenProviderResponse hanaCleaning = world.openProvider.open(new OpenProviderRequest(HANA));

        assertThat(kemasHome.providerId()).isNotEqualTo(hanaCleaning.providerId());
        assertThat(world.providers.count()).isEqualTo(2);
        assertThat(world.providers.stored(new ProviderId(hanaCleaning.providerId())).isRunBy(new PersonId(HANA)))
                .isTrue();
    }

    @Test
    @Tag("PRV-1")
    @Tag("IDN-1")
    @DisplayName("A person Verification hasn't verified as an owner or member can't open a provider.")
    void unverifiedPersonCannotOpenAProvider() {
        assertThatThrownBy(() -> world.openProvider.open(new OpenProviderRequest(AZMAN)))
                .isInstanceOf(PersonNotVerifiedException.class)
                .hasMessageContaining(AZMAN);
        assertThat(world.providers.count()).isZero();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Rina, verified as foreign, can't own a provider under Malaysia's country policy.")
    void foreignPersonCannotOpenAProvider() {
        world.verifiedPeople.save(new VerifiedPerson(
                VerifiedPersonFacts.foreign(new PersonId(RINA), true, RINA_PERMIT_EXPIRY), "Rina"));

        assertThatThrownBy(() -> world.openProvider.open(new OpenProviderRequest(RINA)))
                .isInstanceOf(OwnerNotLocalException.class);
        assertThat(world.providers.count()).isZero();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A person who didn't pass the age check can't own a provider.")
    void underagePersonCannotOpenAProvider() {
        world.verifiedPeople.save(new VerifiedPerson(VerifiedPersonFacts.local(new PersonId(HANA), false), "Hana"));

        assertThatThrownBy(() -> world.openProvider.open(new OpenProviderRequest(HANA)))
                .isInstanceOf(OwnerUnderageException.class);
        assertThat(world.providers.count()).isZero();
    }
}
