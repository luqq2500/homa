package com.homa.conversation.usecase.port.out;

/** Remembers which events each handler has handled, so a redelivered event does nothing (ADR 0002). */
public interface ProcessedMessageStore {

    /** True the first time this handler handles this event; false on a repeat, which must do nothing. */
    boolean recordFirstHandling(String eventId, String handler);
}
