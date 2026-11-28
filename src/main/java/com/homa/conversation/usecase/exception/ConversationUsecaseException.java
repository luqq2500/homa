package com.homa.conversation.usecase.exception;

/** An orchestration failure of a Conversation use case: something is missing, or the actor may not act. */
public abstract class ConversationUsecaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ConversationUsecaseException(String message) {
        super(message);
    }
}
