package com.homa.conversation.domain.exception;

/** A message says something: one with no words is not sent, and an Enquiry cannot be started without its question. */
public final class EmptyMessageException extends ConversationDomainException {

    private static final long serialVersionUID = 1L;

    public EmptyMessageException() {
        super("A message needs some text");
    }
}
