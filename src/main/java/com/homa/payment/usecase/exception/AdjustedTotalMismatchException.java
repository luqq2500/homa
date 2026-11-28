package com.homa.payment.usecase.exception;

import com.homa.kernel.Money;

/**
 * A settlement adjustment says the reservation now costs one amount, but lowering the provider's payout by the refund
 * leaves another: Payment's payout and Reservation's price disagree, because an adjustment was missed, replayed or
 * worked out against other state. Payment never re-derives an amount (plan decision 1), so it moves no money and
 * refuses the adjustment: it is retried and reported until the books are put right.
 */
public class AdjustedTotalMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AdjustedTotalMismatchException(String reservationId, String adjustmentId, Money newTotal, Money payoutGross) {
        super("Adjustment " + adjustmentId + " of reservation " + reservationId + " says the new total is " + newTotal
                + ", but the payout would be " + payoutGross);
    }
}
