package com.homa.conversation.usecase.dto;

/**
 * A party writes a message in a chat (CHT-2, CHT-5).
 *
 * @param conversationId the chat
 * @param senderId       who is signed in and writes, filled by infra/security from the token: the chat's customerId,
 *                       or for the provider the personId of its owner (PRV-1). Rostered members can write for the
 *                       provider once Conversation has the roster (PRV-11, slice 4)
 * @param text           the message, as written
 */
public record SendMessageRequest(String conversationId, String senderId, String text) {
}
