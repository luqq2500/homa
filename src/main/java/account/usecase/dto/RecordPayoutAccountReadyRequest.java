package account.usecase.dto;

/** The business whose payout account Stripe reports as ready. */
public record RecordPayoutAccountReadyRequest(String providerId) {
}
