package com.homa.conversation.usecase.dto;

/**
 * A customer asks a provider a question before reserving (CHT-1).
 *
 * @param customerId the customer who is signed in and asks, filled by infra/security from the token
 * @param providerId the provider they ask
 * @param question   the first message of the Enquiry, as the customer wrote it
 */
public record StartEnquiryRequest(String customerId, String providerId, String question) {
}
