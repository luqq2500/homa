package com.homa.payment.usecase.dto;

/** The scheduler asks for every due payout to be released; the time comes from the TimeSource. */
public record ReleaseDuePayoutsRequest() {
}
