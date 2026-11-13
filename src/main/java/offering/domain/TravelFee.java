package offering.domain;

import kernel.Money;
import offering.domain.exception.InvalidTravelFeeException;

/** How the travel fee works: a separate flat fee of more than RM0, or already included in the price. */
public record TravelFee(boolean separate, Money amount) {

    public TravelFee {
        if (amount == null || separate == amount.isZero()) {
            throw new InvalidTravelFeeException();
        }
    }

    public static TravelFee separate(Money amount) {
        return new TravelFee(true, amount);
    }

    public static TravelFee included() {
        return new TravelFee(false, Money.ZERO);
    }
}
