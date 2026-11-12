package account.usecase.dto;

/** The confirmed phone verification, the customer's name and their street address. */
public record RegisterCustomerRequest(String verificationId, String name, String street) {
}
