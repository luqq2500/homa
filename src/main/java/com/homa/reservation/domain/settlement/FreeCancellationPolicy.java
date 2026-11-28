package com.homa.reservation.domain.settlement;

import java.time.Instant;

/**
 * REQ-2 to REQ-4, CCX-1, CCX-3 to CCX-5: a free outcome. No fee, no fine, no compensation, and the customer gets
 * the whole payment back.
 */
public final class FreeCancellationPolicy implements SettlementPolicy {

    @Override
    public Settlement settle(SettlementBasis basis, Instant at) {
        return basis.settlement(basis.zero(), basis.zero(), basis.zero());
    }
}
