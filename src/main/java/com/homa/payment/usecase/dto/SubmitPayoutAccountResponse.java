package com.homa.payment.usecase.dto;

/**
 * The submitted payout account, waiting for its holder check.
 *
 * @param status PENDING, VERIFIED or NOT_VERIFIED
 */
public record SubmitPayoutAccountResponse(String payoutAccountId, String status) {
}
