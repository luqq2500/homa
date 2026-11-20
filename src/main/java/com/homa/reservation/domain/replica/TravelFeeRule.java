package com.homa.reservation.domain.replica;

import com.homa.kernel.Money;
import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.exception.BookingRefusedException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** OFR-3: the provider's travel fee is none, flat, or by city. A reservation keeps the fee for its city (RES-8). */
public sealed interface TravelFeeRule {

    Money feeFor(String city);

    static TravelFeeRule none() {
        return new NoTravelFee();
    }

    static TravelFeeRule flat(Money fee) {
        return new FlatTravelFee(fee);
    }

    static TravelFeeRule byCity(Map<String, Money> fees) {
        return new TravelFeeByCity(fees);
    }

    record NoTravelFee() implements TravelFeeRule {
        @Override
        public Money feeFor(String city) {
            return Money.zero(Money.MYR);
        }
    }

    record FlatTravelFee(Money fee) implements TravelFeeRule {
        public FlatTravelFee {
            Objects.requireNonNull(fee, "fee");
        }

        @Override
        public Money feeFor(String city) {
            return fee;
        }
    }

    record TravelFeeByCity(Map<String, Money> fees) implements TravelFeeRule {
        public TravelFeeByCity {
            fees = Map.copyOf(fees);
        }

        @Override
        public Money feeFor(String city) {
            return Optional.ofNullable(fees.get(city))
                    .orElseThrow(() -> new BookingRefusedException(BookingRefusal.CITY_NOT_COVERED));
        }
    }
}
