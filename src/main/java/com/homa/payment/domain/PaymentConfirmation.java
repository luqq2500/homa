package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * What a confirmed payment attempt means for its checkout (P1, RES-12): the first confirmed payment counts and is
 * held; any later confirmed payment for the same checkout is refunded in full at once and never held; the same
 * attempt confirmed again changes nothing.
 */
public sealed interface PaymentConfirmation {

    PaymentAttemptId attemptId();

    /** The first confirmed payment for the checkout: it is now held (PAY-1). */
    record Counted(PaymentAttemptId attemptId, Money amount, Instant paidAt) implements PaymentConfirmation {

        public Counted {
            Objects.requireNonNull(attemptId, "attemptId");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(paidAt, "paidAt");
        }
    }

    /** A later confirmed payment for a checkout that already has one: all of it goes back at once (RES-12, PAY-7). */
    record RefundedInFull(Refund refund) implements PaymentConfirmation {

        public RefundedInFull {
            Objects.requireNonNull(refund, "refund");
        }

        @Override
        public PaymentAttemptId attemptId() {
            return refund.attemptId();
        }
    }

    /** This attempt's confirmation was already recorded: nothing is held or refunded a second time. */
    record AlreadyRecorded(PaymentAttemptId attemptId) implements PaymentConfirmation {

        public AlreadyRecorded {
            Objects.requireNonNull(attemptId, "attemptId");
        }
    }
}
