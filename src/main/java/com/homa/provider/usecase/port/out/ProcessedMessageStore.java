package com.homa.provider.usecase.port.out;

/** Which events each handler has already handled, so a redelivered event is a no-op (ADR 0002). */
public interface ProcessedMessageStore {

    /** True the first time this handler handles this event; false on a repeat, which must do nothing. */
    boolean recordFirstHandling(String eventId, String handler);
}
