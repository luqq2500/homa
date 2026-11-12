package account.usecase.dto;

/** The customer's name, phone number and address. */
public record CustomerViewAccountResponse(String customerId, String name, String phone, String street, String city) {
}
