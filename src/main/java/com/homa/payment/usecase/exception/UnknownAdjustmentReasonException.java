package com.homa.payment.usecase.exception;

/**
 * A settlement adjustment names a reason Payment has no refund reason for, so its refund could not be told apart from
 * another. Nothing is sent: it is retried and reported until Payment knows the reason.
 */
public class UnknownAdjustmentReasonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnknownAdjustmentReasonException(String reason) {
        super("Payment has no refund reason for the settlement adjustment reason " + reason);
    }
}
