package account.usecase.dto;

/**
 * A business's account as it sees it.
 *
 * @param verification {@code PENDING}, {@code VERIFIED}, {@code REJECTED} or {@code LAPSED}
 * @param ssmNumber the SSM number from KYB, or empty before KYB passes
 * @param registeredName the registered business name from KYB, or empty before KYB passes
 * @param verifiedUntil the date the business is verified until, written as an ISO-8601 instant, or empty
 *     before KYB passes
 * @param readyForGoLive verified and with a ready payout account: the business's own part of going live
 */
public record ProviderViewAccountResponse(
        String providerId,
        String phone,
        String verification,
        String ssmNumber,
        String registeredName,
        String verifiedUntil,
        boolean payoutAccountReady,
        boolean readyForGoLive) {
}
