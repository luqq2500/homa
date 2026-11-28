package com.homa.conversation.domain;

/** Why a chat can be read but not written to (CHT-5). */
public enum ChatReadOnlyReason {
    /** The reservation's Protection Window has ended. */
    PROTECTION_WINDOW_ENDED,
    /** The customer withdrew the reservation. */
    RESERVATION_WITHDRAWN,
    /** The provider declined the reservation. */
    RESERVATION_DECLINED,
    /** The provider did not answer by the acceptance deadline. */
    RESERVATION_EXPIRED,
    /** The reservation was cancelled before its Check-in Period started. */
    RESERVATION_CANCELLED_BEFORE_CHECK_IN
}
