package com.homa.app.outbox.sample.conversation.usecase.port.out;

/**
 * The pattern every consuming context copies into its own usecase/port/out (docs/decisions/0002): remembers which
 * events each of the context's handlers has handled, in the handler's transaction.
 */
public interface ProcessedMessageStore {

    /**
     * Records that {@code handler} handled {@code eventId}.
     *
     * @return true the first time, false when the handler already handled this event and must do nothing
     */
    boolean recordFirstHandling(String eventId, String handler);
}
