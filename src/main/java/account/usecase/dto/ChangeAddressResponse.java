package account.usecase.dto;

/** The address as it now is, with the city the geoservice found. */
public record ChangeAddressResponse(String street, String city) {
}
