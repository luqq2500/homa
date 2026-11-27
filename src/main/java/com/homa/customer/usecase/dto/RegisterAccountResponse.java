package com.homa.customer.usecase.dto;

/**
 * The new customer account.
 *
 * @param customerId the new customer's id
 * @param status     the customer's standing: ACTIVE for a new customer, who still verifies before reserving (CUS-3)
 */
public record RegisterAccountResponse(String customerId, String status) {
}
