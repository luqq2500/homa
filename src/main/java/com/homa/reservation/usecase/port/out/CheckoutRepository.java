package com.homa.reservation.usecase.port.out;

import com.homa.reservation.domain.Checkout;
import com.homa.reservation.domain.CheckoutId;
import java.util.Optional;

/** Checkouts: the booking and the price a customer pays, kept until the payment is confirmed (RES-11). */
public interface CheckoutRepository {

    Optional<Checkout> findById(CheckoutId id);

    void save(Checkout checkout);
}
