package com.homa.payment.usecase.port.out;

import com.homa.payment.domain.Payout;
import com.homa.payment.domain.ReservationId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Stores the providers' payouts, one per reservation. */
public interface PayoutRepository {

    Optional<Payout> findByReservationId(ReservationId reservationId);

    /**
     * Every PENDING payout whose Protection Window ended at or before {@code now}, oldest window first. A query
     * shortcut only: the use case still asks each payout whether it is releasable (PAY-2, PAY-6, RPT-6).
     */
    List<Payout> findPendingWithProtectionWindowEndedBy(Instant now);

    /** Inserts or updates the payout, with its open reports and its release once released. */
    void save(Payout payout);
}
