package com.homa.reservation.domain.terms;

import com.homa.reservation.domain.ReservationValues.Version;
import java.time.Duration;

/** §4.3 Reservation Acceptance Policies. A provider chooses exactly one (OFR-9). */
public enum AcceptancePolicy {

    /** ACC-1: Reserved immediately after it is Requested; it must be made before the Check-in Period starts. */
    INSTANT {
        @Override
        public boolean permitsNotice(Duration notice, Version values) {
            return notice.isPositive();
        }

        @Override
        public boolean reservesAtOnce() {
            return true;
        }
    },

    /**
     * ACC-2, ACC-3: Reserved only when the provider accepts before the acceptance deadline; it must be made at least
     * 1 hour before the Check-in Period starts.
     */
    REQUEST {
        @Override
        public boolean permitsNotice(Duration notice, Version values) {
            return notice.compareTo(values.acceptanceCutOffBeforeCheckIn()) >= 0;
        }

        @Override
        public boolean reservesAtOnce() {
            return false;
        }
    };

    /** Whether a reservation can be made with this much time left before the Check-in Period starts. */
    public abstract boolean permitsNotice(Duration notice, Version values);

    /** Whether the reservation is Reserved as soon as it is Requested. */
    public abstract boolean reservesAtOnce();
}
