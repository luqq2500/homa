package com.homa.conversation.usecase.exception;

/** CHT-1: only a customer can start a chat, and Conversation knows no customer with this id. */
public final class CustomerNotFoundException extends ConversationUsecaseException {

    private static final long serialVersionUID = 1L;

    public CustomerNotFoundException(String customerId) {
        super("CHT-1: no customer " + customerId + ", and only a customer can start a chat");
    }
}
