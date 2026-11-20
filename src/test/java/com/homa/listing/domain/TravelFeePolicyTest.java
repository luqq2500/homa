package com.homa.listing.domain;

import static com.homa.listing.domain.ListingFixtures.CYBERJAYA;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_TRAVEL_FEE;
import static com.homa.listing.domain.ListingFixtures.PUTRAJAYA;
import static com.homa.listing.domain.ListingFixtures.SALAK_TINGGI;
import static com.homa.listing.domain.ListingFixtures.kemasHomeCities;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.listing.domain.exception.InvalidTravelFeeException;
import com.homa.listing.domain.exception.UnpricedCityException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-3")
class TravelFeePolicyTest {

    private static final TravelFeePolicy BY_CITY = TravelFeePolicy.byCity(Map.of(
            SALAK_TINGGI, Money.myr(10), PUTRAJAYA, Money.myr(25)));

    @Test
    @DisplayName("Kemas Home sets RM20 flat. Another provider sets RM10 for Salak Tinggi and RM25 for Putrajaya.")
    void aTravelFeeIsFlatOrByCity() {
        assertThat(KEMAS_HOME_TRAVEL_FEE.feeFor(SALAK_TINGGI)).isEqualTo(Money.myr(20));
        assertThat(KEMAS_HOME_TRAVEL_FEE.feeFor(PUTRAJAYA)).isEqualTo(Money.myr(20));
        assertThat(BY_CITY.feeFor(SALAK_TINGGI)).isEqualTo(Money.myr(10));
        assertThat(BY_CITY.feeFor(PUTRAJAYA)).isEqualTo(Money.myr(25));
    }

    @Test
    @DisplayName("A provider with no travel fee charges nothing in any city")
    void noTravelFeeChargesNothing() {
        assertThat(TravelFeePolicy.none().feeFor(CYBERJAYA)).isEqualTo(Money.zero(Money.MYR));
        assertThat(TravelFeePolicy.none().unpricedAmong(kemasHomeCities())).isEmpty();
    }

    @Test
    @DisplayName("A flat fee prices every covered city; a by-city fee names the covered cities it doesn't price")
    void aByCityFeeNamesTheCoveredCitiesItDoesntPrice() {
        assertThat(KEMAS_HOME_TRAVEL_FEE.unpricedAmong(kemasHomeCities())).isEmpty();
        assertThat(BY_CITY.unpricedAmong(kemasHomeCities())).containsExactly(CYBERJAYA);
        assertThatThrownBy(() -> BY_CITY.feeFor(CYBERJAYA)).isInstanceOf(UnpricedCityException.class)
                .hasMessageContaining("CYBERJAYA");
    }

    @Test
    @DisplayName("A travel fee can't be negative; free (RM0) is allowed")
    void aTravelFeeCantBeNegative() {
        assertThat(TravelFeePolicy.flat(Money.myr(0)).feeFor(SALAK_TINGGI)).isEqualTo(Money.myr(0));
        assertThat(TravelFeePolicy.byCity(Map.of(SALAK_TINGGI, Money.myr(0))).feeFor(SALAK_TINGGI)).isEqualTo(Money.myr(0));
        assertThatThrownBy(() -> TravelFeePolicy.flat(Money.myr(-1))).isInstanceOf(InvalidTravelFeeException.class);
        assertThatThrownBy(() -> TravelFeePolicy.byCity(Map.of(SALAK_TINGGI, Money.myr("-0.01"))))
                .isInstanceOf(InvalidTravelFeeException.class);
        assertThatThrownBy(() -> TravelFeePolicy.byCity(Map.of())).isInstanceOf(InvalidTravelFeeException.class);
        assertThatThrownBy(() -> TravelFeePolicy.flat(null)).isInstanceOf(NullPointerException.class);
    }
}
