package com.homa.listing.usecase;

import com.homa.kernel.Money;
import com.homa.listing.domain.City;
import com.homa.listing.domain.TravelFeePolicy;
import java.util.Map;
import java.util.stream.Collectors;

/** The three travel fee kinds as requests and {@code ListingTermsChangedIE} name them (OFR-3). */
enum TravelFeeKind {
    NONE,
    FLAT,
    BY_CITY;

    static TravelFeeKind of(TravelFeePolicy travelFee) {
        return switch (travelFee) {
            case TravelFeePolicy.None none -> NONE;
            case TravelFeePolicy.Flat flat -> FLAT;
            case TravelFeePolicy.ByCity byCity -> BY_CITY;
        };
    }

    /** The policy of this kind; a flat fee reads only the flat fee, a by-city fee only the fees by city code. */
    TravelFeePolicy policy(Money flatFee, Map<String, Money> feesByCity) {
        return switch (this) {
            case NONE -> TravelFeePolicy.none();
            case FLAT -> TravelFeePolicy.flat(flatFee);
            case BY_CITY -> TravelFeePolicy.byCity(feesByCity.entrySet().stream()
                    .collect(Collectors.toMap(e -> new City(e.getKey()), Map.Entry::getValue)));
        };
    }
}
