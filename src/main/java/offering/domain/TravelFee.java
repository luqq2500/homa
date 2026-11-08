package offering.domain;

import kernel.Money;
import offering.domain.exception.TravelFeeNotPositiveException;
import java.util.Optional;

/** How a provider charges for travel: a separate flat fee per booking, or already included in the hourly rate. */
public record TravelFee(Optional<Money> separateAmount) {

    public TravelFee {
        separateAmount.ifPresent(TravelFee::requirePositive);
    }

    public static TravelFee separate(Money amount) {
        return new TravelFee(Optional.of(amount));
    }

    public static TravelFee included() {
        return new TravelFee(Optional.empty());
    }

    public boolean isIncluded() {
        return separateAmount.isEmpty();
    }

    private static void requirePositive(Money amount) {
        if (amount.isNegative() || amount.isZero()) {
            throw new TravelFeeNotPositiveException(amount);
        }
    }
}
