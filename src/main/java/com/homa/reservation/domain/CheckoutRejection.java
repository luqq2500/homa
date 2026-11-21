package com.homa.reservation.domain;

/**
 * Why a confirmed payment could not become a reservation, as the customer is told and Payment refunds it in full at
 * once (P1, RES-11).
 */
public enum CheckoutRejection {
    /** Another customer's payment for the provider's time was confirmed first. */
    TIME_TAKEN,
    /** A booking rule no longer passes. */
    RULE_FAILED
}
