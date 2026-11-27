package com.homa.customer.domain.exception;

import com.homa.customer.domain.SuspensionLength;
import java.time.Instant;
import java.util.Objects;

/**
 * CUS-6: a 7- or 30-day suspension ends by itself, so Support's decision gives an end after it starts; a suspension
 * until lifted has no end of its own. Raised when the end Support gave doesn't fit the length.
 */
public class InconsistentSuspensionEndException extends RuntimeException {

    public InconsistentSuspensionEndException(SuspensionLength length, Instant endsAt) {
        super(length.endsOnItsOwn()
                ? "CUS-6: a " + length + " suspension ends on its own, so it needs an end after it starts, not "
                        + Objects.toString(endsAt, "none")
                : "CUS-6: a suspension until lifted has no end of its own, so it can't end at " + endsAt);
    }
}
