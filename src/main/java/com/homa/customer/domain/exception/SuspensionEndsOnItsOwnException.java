package com.homa.customer.domain.exception;

import com.homa.customer.domain.SuspensionLength;
import java.time.Instant;

/**
 * CUS-6: a 7- or 30-day suspension ends by itself at the end Support gave. Support lifts only a suspension until
 * lifted.
 */
public class SuspensionEndsOnItsOwnException extends RuntimeException {

    public SuspensionEndsOnItsOwnException(SuspensionLength length, Instant endsAt) {
        super("CUS-6: a " + length + " suspension ends on its own at " + endsAt
                + "; Support lifts only a suspension until lifted");
    }
}
