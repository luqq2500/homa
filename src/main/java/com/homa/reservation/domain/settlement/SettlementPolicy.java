package com.homa.reservation.domain.settlement;

import java.time.Instant;

/**
 * One money rule of §6 and §7, as a small strategy: from what a reservation was booked with and the moment the rule
 * applies, the {@link Settlement}. A policy reads only the {@link SettlementBasis}, so it can never see the live
 * listing or a value changed after the reservation was Requested (P3).
 */
public interface SettlementPolicy {

    Settlement settle(SettlementBasis basis, Instant at);
}
