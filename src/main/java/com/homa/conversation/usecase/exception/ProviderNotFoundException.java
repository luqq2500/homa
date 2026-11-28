package com.homa.conversation.usecase.exception;

/** Conversation knows no provider with this id, so there is no one to ask. */
public final class ProviderNotFoundException extends ConversationUsecaseException {

    private static final long serialVersionUID = 1L;

    public ProviderNotFoundException(String providerId) {
        super("No provider " + providerId);
    }
}
