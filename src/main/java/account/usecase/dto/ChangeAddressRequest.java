package account.usecase.dto;

/** The customer, from authentication, and their new street address. */
public record ChangeAddressRequest(String customerId, String street) {
}
