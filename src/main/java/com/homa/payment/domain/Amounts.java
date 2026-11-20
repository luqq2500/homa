package com.homa.payment.domain;

import com.homa.kernel.Money;
import com.homa.payment.domain.exception.AmountNotPayableException;
import java.util.Objects;

/** The one check every amount Payment moves makes: positive, and already rounded to the sen (PAY-8). */
final class Amounts {

    private Amounts() {
    }

    static Money requirePayable(Money amount, String what) {
        Objects.requireNonNull(amount, what);
        if (!amount.isPositive() || !amount.equals(amount.roundedToSen())) {
            throw new AmountNotPayableException(what, amount);
        }
        return amount;
    }
}
