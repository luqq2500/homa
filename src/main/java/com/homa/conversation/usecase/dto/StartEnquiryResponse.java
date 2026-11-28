package com.homa.conversation.usecase.dto;

import java.time.Instant;

/**
 * The Enquiry that was started, with the question that opened it.
 *
 * @param conversationId the new Enquiry
 * @param messageId      the customer's question, its first message
 * @param startedAt      when the question was sent
 */
public record StartEnquiryResponse(String conversationId, String messageId, Instant startedAt) {
}
