package payment.domain;

import kernel.Money;
import payment.domain.exception.PaymentDetailRequiredException;

/** Rules shared across payment objects. */
final class PaymentText {

    private PaymentText() {
    }

    static String require(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new PaymentDetailRequiredException(what);
        }
        return text.strip();
    }

    static boolean isPositive(Money amount) {
        return !amount.isNegative() && !amount.isZero();
    }
}
