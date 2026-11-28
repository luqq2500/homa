package com.homa.conversation.usecase.dto;

/**
 * A party reads a chat.
 *
 * @param conversationId the chat
 * @param viewerId       who is signed in and reads, filled by infra/security from the token: the chat's customerId,
 *                       or for the provider the personId of its owner (PRV-1)
 */
public record ViewConversationRequest(String conversationId, String viewerId) {
}
