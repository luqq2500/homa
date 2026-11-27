package com.homa.payment.usecase.port.out;

/** Makes every handler idempotent (ADR 0002): consumed events and gateway webhooks alike. */
public interface ProcessedMessageStore {

    /** True the first time this handler handles this event; false on a repeat, which must do nothing. */
    boolean recordFirstHandling(String eventId, String handler);
}
