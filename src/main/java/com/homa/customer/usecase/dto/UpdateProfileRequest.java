package com.homa.customer.usecase.dto;

/**
 * The customer's whole profile as they save it: every detail CUS-1 asks for, changed or not.
 *
 * @param signedInCustomerId the customer who is signed in, filled by infra/security from the token
 * @param customerId         the customer whose profile changes; only the signed-in customer's own
 * @param name               the name the customer goes by
 * @param streetAddress      the home address
 * @param city               the Homa city code of the home address
 * @param arrivalNote        how the team reaches the home
 */
public record UpdateProfileRequest(
        String signedInCustomerId,
        String customerId,
        String name,
        String streetAddress,
        String city,
        String arrivalNote) {
}
