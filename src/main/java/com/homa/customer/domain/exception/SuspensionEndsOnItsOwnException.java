package com.homa.customer.domain.exception;

import com.homa.customer.domain.SuspensionPeriod;
import java.time.Instant;

/**
 * CUS-6: a suspension for a set period ends on its own when the period runs out. Support lifts only a suspension
 * until lifted.
 */
public class SuspensionEndsOnItsOwnException extends RuntimeException {

    public SuspensionEndsOnItsOwnException(SuspensionPeriod period, Instant endsAt) {
        super("CUS-6: a " + period + " suspension ends on its own at " + endsAt
                + "; Support lifts only a suspension until lifted");
    }
}
