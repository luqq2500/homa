package com.homa.conversation.usecase.exception;

/**
 * A reservation's chat has no window to judge it by. A reservation's chat and its window are stored together, so
 * this means the stored data is incomplete, not that the caller did anything wrong.
 */
public final class ReservationChatWindowNotFoundException extends ConversationUsecaseException {

    private static final long serialVersionUID = 1L;

    public ReservationChatWindowNotFoundException(String reservationId) {
        super("No chat window for reservation " + reservationId);
    }
}
