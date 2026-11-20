package com.homa.listing.domain;

import static com.homa.listing.domain.ListingFixtures.HEPA_VACUUM;
import static com.homa.listing.domain.ListingFixtures.KITCHEN_CLEANING;
import static com.homa.listing.domain.ListingFixtures.SALAK_TINGGI;
import static com.homa.listing.domain.ListingFixtures.SHAH_ALAM;
import static com.homa.listing.domain.ListingFixtures.kemasHomeOfferings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.listing.domain.exception.ListingRuleViolationException;
import com.homa.listing.domain.exception.NothingOfferedException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-1")
class OfferingsTest {

    @Test
    @DisplayName("Kemas Home covers Salak Tinggi but not Shah Alam")
    void offeringsCoverOnlyTheChosenCities() {
        assertThat(kemasHomeOfferings().covers(SALAK_TINGGI)).isTrue();
        assertThat(kemasHomeOfferings().covers(SHAH_ALAM)).isFalse();
    }

    @Test
    @DisplayName("A listing must cover at least one job and one city; tools are optional")
    void offeringsNeedAJobAndACityButNoTool() {
        assertThat(new Offerings(Set.of(KITCHEN_CLEANING), Set.of(), Set.of(SALAK_TINGGI)).tools()).isEmpty();
        assertThatThrownBy(() -> new Offerings(Set.of(), Set.of(HEPA_VACUUM), Set.of(SALAK_TINGGI)))
                .isInstanceOf(NothingOfferedException.class)
                .extracting(e -> ((ListingRuleViolationException) e).ruleId()).isEqualTo("OFR-1");
        assertThatThrownBy(() -> new Offerings(Set.of(KITCHEN_CLEANING), Set.of(HEPA_VACUUM), Set.of()))
                .isInstanceOf(NothingOfferedException.class).hasMessageContaining("city");
    }

    @Test
    @DisplayName("Offerings keep their own copy of the provider's choices")
    void offeringsKeepTheirOwnCopy() {
        Set<City> cities = new HashSet<>(Set.of(SALAK_TINGGI));
        Offerings offerings = new Offerings(Set.of(KITCHEN_CLEANING), Set.of(), cities);

        cities.add(SHAH_ALAM);

        assertThat(offerings.cities()).containsExactly(SALAK_TINGGI);
    }
}
