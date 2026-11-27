package com.homa.listing.usecase.port.out;

/** The events each Listing handler has already handled (ADR 0002). */
public interface ProcessedMessageStore {

    /** True the first time this handler handles this event; false on a repeat, which must do nothing. */
    boolean recordFirstHandling(String eventId, String handler);
}
