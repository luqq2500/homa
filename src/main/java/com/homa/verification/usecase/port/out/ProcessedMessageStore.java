package com.homa.verification.usecase.port.out;

/**
 * Records which events and service notifications a handler has already handled (docs/decisions/0002), so a
 * redelivered event or a repeated webhook is a no-op.
 */
public interface ProcessedMessageStore {

    /** True the first time this handler handles this message; false on a repeat, which must do nothing. */
    boolean recordFirstHandling(String eventId, String handler);
}
