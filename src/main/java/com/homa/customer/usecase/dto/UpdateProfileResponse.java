package com.homa.customer.usecase.dto;

/** The customer's profile as saved (CUS-1). */
public record UpdateProfileResponse(
        String customerId,
        String name,
        String streetAddress,
        String city,
        String arrivalNote) {
}
