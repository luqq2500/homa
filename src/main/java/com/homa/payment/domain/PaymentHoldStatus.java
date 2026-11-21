package com.homa.payment.domain;

/** Where a customer's payment for a checkout stands. */
public enum PaymentHoldStatus {
    /** No payment is confirmed yet. A failed attempt leaves the checkout here, so the customer can retry. */
    PENDING,
    /** The first confirmed payment is held by Homa (PAY-1). The provider cannot receive it yet. */
    HELD,
    /** Everything that was held has been refunded to the customer. */
    REFUNDED
}
