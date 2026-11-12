package account.usecase.dto;

/** The business's account as registered, waiting for KYB. */
public record RegisterProviderResponse(String providerId, String phone) {
}
