package com.homa.conversation.domain.exception;

/** A Conversation business rule was broken. Every domain exception of the Conversation context extends this. */
public abstract class ConversationDomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ConversationDomainException(String message) {
        super(message);
    }
}
