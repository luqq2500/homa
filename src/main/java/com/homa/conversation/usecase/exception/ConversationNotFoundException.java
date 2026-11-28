package com.homa.conversation.usecase.exception;

/** No conversation has this id. */
public final class ConversationNotFoundException extends ConversationUsecaseException {

    private static final long serialVersionUID = 1L;

    public ConversationNotFoundException(String conversationId) {
        super("No conversation " + conversationId);
    }
}
