package com.homa.conversation.usecase.exception;

/** The actor is neither the customer of the conversation nor the person who speaks for its provider. */
public final class NotConversationPartyException extends ConversationUsecaseException {

    private static final long serialVersionUID = 1L;

    public NotConversationPartyException(String actorId, String conversationId) {
        super(actorId + " is not a party of conversation " + conversationId);
    }
}
