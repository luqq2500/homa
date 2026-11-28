package com.homa.rating.domain;

import static com.homa.rating.domain.RatingFixtures.AISYAH;
import static com.homa.rating.domain.RatingFixtures.AISYAH_PARTY;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_PARTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PartyTest {

    private static final Party HANA_CLEANING = Party.provider(new ProviderId("provider-hana-cleaning"));
    private static final Party DANIEL = Party.customer(new CustomerId("customer-daniel"));

    @Test
    @Tag("RTG-6")
    @DisplayName("Other customers see Aisyah's 4 stars on Kemas Home's page; Kemas Home sees its own ratings.")
    void aProvidersRatingsAreSeenByItAndByCustomers() {
        assertThat(KEMAS_HOME_PARTY.canBeSeenBy(AISYAH_PARTY)).isTrue();
        assertThat(KEMAS_HOME_PARTY.canBeSeenBy(DANIEL)).isTrue();
        assertThat(KEMAS_HOME_PARTY.canBeSeenBy(KEMAS_HOME_PARTY)).isTrue();
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("Hana Cleaning, another provider, doesn't see Kemas Home's ratings.")
    void anotherProviderDoesntSeeAProvidersRatings() {
        assertThat(KEMAS_HOME_PARTY.canBeSeenBy(HANA_CLEANING)).isFalse();
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("A customer's ratings are seen by them and by providers, not by other customers.")
    void aCustomersRatingsAreSeenByThemAndByProviders() {
        assertThat(AISYAH_PARTY.canBeSeenBy(AISYAH_PARTY)).isTrue();
        assertThat(AISYAH_PARTY.canBeSeenBy(KEMAS_HOME_PARTY)).isTrue();
        assertThat(AISYAH_PARTY.canBeSeenBy(HANA_CLEANING)).isTrue();
        assertThat(AISYAH_PARTY.canBeSeenBy(DANIEL)).isFalse();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Reliability is seen the same way: providers see a customer's, customers see a provider's.")
    void reliabilityIsSeenTheSameWay() {
        assertThat(KEMAS_HOME_PARTY.canBeSeenBy(AISYAH_PARTY)).isTrue();
        assertThat(AISYAH_PARTY.canBeSeenBy(KEMAS_HOME_PARTY)).isTrue();
        assertThat(AISYAH_PARTY.canBeSeenBy(DANIEL)).isFalse();
    }

    @Test
    @Tag("RTG-1")
    @DisplayName("A party is a customer or a provider with an id, made from Aisyah's or Kemas Home's own id.")
    void aPartyIsOneSideWithItsId() {
        assertThat(Party.customer(AISYAH)).isEqualTo(new Party(PartyRole.CUSTOMER, AISYAH.value()));
        assertThat(Party.provider(KEMAS_HOME)).isEqualTo(new Party(PartyRole.PROVIDER, KEMAS_HOME.value()));
        assertThatThrownBy(() -> new Party(null, "x")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Party(PartyRole.CUSTOMER, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AISYAH_PARTY.canBeSeenBy(null)).isInstanceOf(NullPointerException.class);
    }
}
