package com.homa.conversation.domain.exception;

import com.homa.conversation.domain.ChatReadOnlyReason;

/** CHT-2, CHT-5: the chat can be read but not written to. New questions go in a new Enquiry. */
public final class ChatReadOnlyException extends ConversationDomainException {

    private static final long serialVersionUID = 1L;

    private final ChatReadOnlyReason reason;

    public ChatReadOnlyException(ChatReadOnlyReason reason) {
        super("CHT-5: this chat is read-only (" + reason + "); new questions go in a new Enquiry");
        this.reason = reason;
    }

    public ChatReadOnlyReason reason() {
        return reason;
    }
}
