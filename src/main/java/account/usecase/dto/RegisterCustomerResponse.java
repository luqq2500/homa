package account.usecase.dto;

/** The customer's account as registered, with the city the geoservice found. */
public record RegisterCustomerResponse(String customerId, String name, String street, String city) {
}
