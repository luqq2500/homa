package com.homa.payment.domain;

/** Where a customer's payment for a checkout stands. */
public enum PaymentHoldStatus {
    /** The checkout has started and Payment is waiting for the gateway to secure the payment. */
    PENDING,
    /** The payment is secured and held by Homa (PAY-1). The provider cannot receive it yet. */
    HELD,
    /** Everything that was held has been refunded to the customer. */
    REFUNDED
}
