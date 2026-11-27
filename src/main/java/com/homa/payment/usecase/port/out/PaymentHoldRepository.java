package com.homa.payment.usecase.port.out;

import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.PaymentHold;
import java.util.Optional;

/** Stores the customer's payment holds, one per checkout (RES-11). */
public interface PaymentHoldRepository {

    Optional<PaymentHold> findByCheckoutId(CheckoutId checkoutId);

    /** Inserts or updates the hold, with every payment attempt it has seen confirmed. */
    void save(PaymentHold hold);
}
