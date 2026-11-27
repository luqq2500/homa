package com.homa.payment.usecase;

import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutStatus;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.port.out.PayoutRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test only: payouts by reservation, and every save in order. The pending query mirrors the SQL infra will run. */
public class InMemoryPayoutRepository implements PayoutRepository {

    private final Map<ReservationId, Payout> payouts = new LinkedHashMap<>();
    private final List<Payout> saves = new ArrayList<>();

    /** Puts a payout in place without counting it as a save. */
    public void given(Payout payout) {
        payouts.put(payout.reservationId(), payout);
    }

    @Override
    public Optional<Payout> findByReservationId(ReservationId reservationId) {
        return Optional.ofNullable(payouts.get(reservationId));
    }

    @Override
    public List<Payout> findPendingWithProtectionWindowEndedBy(Instant now) {
        return payouts.values().stream()
                .filter(payout -> payout.status() == PayoutStatus.PENDING)
                .filter(payout -> !payout.protectionWindowEnd().isAfter(now))
                .sorted(Comparator.comparing(Payout::protectionWindowEnd))
                .toList();
    }

    @Override
    public void save(Payout payout) {
        payouts.put(payout.reservationId(), payout);
        saves.add(payout);
    }

    public List<Payout> saves() {
        return List.copyOf(saves);
    }

    public List<Payout> all() {
        return List.copyOf(payouts.values());
    }
}
