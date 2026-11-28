package com.homa.payment.usecase;

import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test only: holds by checkout, and every save in order, so a test can tell a change was saved. */
public class InMemoryPaymentHoldRepository implements PaymentHoldRepository {

    private final Map<CheckoutId, PaymentHold> holds = new LinkedHashMap<>();
    private final List<PaymentHold> saves = new ArrayList<>();

    /** Puts a hold in place without counting it as a save. */
    public void given(PaymentHold hold) {
        holds.put(hold.checkoutId(), hold);
    }

    @Override
    public Optional<PaymentHold> findByCheckoutId(CheckoutId checkoutId) {
        return Optional.ofNullable(holds.get(checkoutId));
    }

    @Override
    public Optional<PaymentHold> findByReservationId(ReservationId reservationId) {
        return holds.values().stream()
                .filter(hold -> hold.reservationId().filter(reservationId::equals).isPresent())
                .findFirst();
    }

    @Override
    public void save(PaymentHold hold) {
        holds.put(hold.checkoutId(), hold);
        saves.add(hold);
    }

    public List<PaymentHold> saves() {
        return List.copyOf(saves);
    }
}
