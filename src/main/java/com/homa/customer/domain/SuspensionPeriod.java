package com.homa.customer.domain;

import com.homa.kernel.BusinessCalendar;
import java.time.Instant;
import java.util.Optional;

/**
 * How long Support suspends a customer for (CUS-4): a set number of calendar days, or until Support lifts it. The
 * lengths are {@link CustomerValues}.
 */
public enum SuspensionPeriod {

    /** Ends on its own {@link CustomerValues#SHORT_SUSPENSION_DAYS} calendar days after it starts (CUS-6). */
    SEVEN_DAYS {
        @Override
        Optional<Instant> endsAt(Instant suspendedAt) {
            return Optional.of(BusinessCalendar.plusDays(suspendedAt, CustomerValues.SHORT_SUSPENSION_DAYS));
        }
    },

    /** Ends on its own {@link CustomerValues#LONG_SUSPENSION_DAYS} calendar days after it starts (CUS-6). */
    THIRTY_DAYS {
        @Override
        Optional<Instant> endsAt(Instant suspendedAt) {
            return Optional.of(BusinessCalendar.plusDays(suspendedAt, CustomerValues.LONG_SUSPENSION_DAYS));
        }
    },

    /** Has no end of its own; it ends only when Support lifts it (CUS-6). */
    UNTIL_LIFTED {
        @Override
        Optional<Instant> endsAt(Instant suspendedAt) {
            return Optional.empty();
        }
    };

    /**
     * When a suspension of this period that starts at {@code suspendedAt} runs out: the same Kuala Lumpur
     * wall-clock time that many calendar days later. Empty for a suspension until lifted.
     */
    abstract Optional<Instant> endsAt(Instant suspendedAt);
}
