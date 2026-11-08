package reservation.domain;

import kernel.Money;

/** How an edit changed the total: positive when the customer pays more, negative when they get money back. */
public record PriceChange(Money previousTotal, Money newTotal) {

    public Money difference() {
        return newTotal.minus(previousTotal);
    }
}
