package reservation.domain;

import kernel.Money;

/**
 * How a reservation's money is split: what the provider receives and what for. The customer is
 * refunded the rest, and the service fee follows what the provider finally receives.
 */
public record Settlement(Money providerReceives, PayoutKind kind) {

    public static Settlement fullRefund() {
        return new Settlement(Money.ZERO, PayoutKind.JOB);
    }
}
