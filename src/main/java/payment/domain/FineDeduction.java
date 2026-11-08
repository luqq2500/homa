package payment.domain;

import kernel.Money;

/** The part of one fine taken off a payout. */
public record FineDeduction(String fineId, Money amount) {
}
