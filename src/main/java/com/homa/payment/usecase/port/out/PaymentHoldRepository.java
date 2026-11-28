package com.homa.payment.usecase.port.out;

import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.ReservationId;
import java.util.Optional;

/** Stores the customer's payment holds, one per checkout (RES-11), linked to the reservation once it is Requested. */
public interface PaymentHoldRepository {

    Optional<PaymentHold> findByCheckoutId(CheckoutId checkoutId);

    /**
     * The hold of a Requested reservation, which later events about the reservation name instead of the checkout
     * (REQ-2 to REQ-4, PAY-4, PAY-7). Empty until the hold is linked to the reservation.
     */
    Optional<PaymentHold> findByReservationId(ReservationId reservationId);

    /** Inserts or updates the hold, with every payment attempt it has seen confirmed and the reservation it is linked to. */
    void save(PaymentHold hold);
}
