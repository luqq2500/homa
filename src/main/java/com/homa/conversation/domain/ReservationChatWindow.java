package com.homa.conversation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * What decides whether a reservation's chat can be written to (CHT-2, CHT-5): the reservation's status as far as the
 * chat cares, and the end of its Protection Window. Conversation holds it as a replica, kept up to date from
 * Reservation's events, and works the rest out from the clock, so no scheduler turns a chat read-only and no
 * "locked" flag is stored.
 *
 * <p>The chat is open from Requested until the Protection Window ends. It is read-only at once when the reservation
 * is Withdrawn, Declined or Expired, or is cancelled before its Check-in Period starts. A cancellation once the
 * Check-in Period has started changes nothing: the chat stays open until the Protection Window ends. Whichever
 * ending is heard first stands, and an ended reservation never reopens.
 *
 * <p>Reservation's events about one reservation arrive in order, so the request is heard of first. A replica is
 * still built from any of them, so an event that comes early is kept, never lost: its end is recorded without the
 * Protection Window end (which Withdrawn, Declined and Expired don't carry), and the request fills it in later.
 */
public final class ReservationChatWindow {

    private final ReservationId reservationId;
    private final CustomerId customerId;
    private final ProviderId providerId;
    private final ChatWindowStatus status;
    private final Instant protectionWindowEnd;

    private ReservationChatWindow(ReservationId reservationId, CustomerId customerId, ProviderId providerId,
            ChatWindowStatus status, Instant protectionWindowEnd) {
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.status = Objects.requireNonNull(status, "status");
        this.protectionWindowEnd = protectionWindowEnd;
    }

    /** CHT-2: the reservation is Requested, so its chat is open until its Protection Window ends. */
    public static ReservationChatWindow open(ReservationId reservationId, CustomerId customerId,
            ProviderId providerId, Instant protectionWindowEnd) {
        return new ReservationChatWindow(reservationId, customerId, providerId, ChatWindowStatus.OPEN,
                Objects.requireNonNull(protectionWindowEnd, "protectionWindowEnd"));
    }

    /** CHT-5: the withdrawal is heard before the request. Read-only at once; the request fills in the window end. */
    public static ReservationChatWindow withdrawnBeforeRequested(ReservationId reservationId, CustomerId customerId,
            ProviderId providerId) {
        return new ReservationChatWindow(reservationId, customerId, providerId, ChatWindowStatus.WITHDRAWN, null);
    }

    /** CHT-5: the decline is heard before the request. Read-only at once; the request fills in the window end. */
    public static ReservationChatWindow declinedBeforeRequested(ReservationId reservationId, CustomerId customerId,
            ProviderId providerId) {
        return new ReservationChatWindow(reservationId, customerId, providerId, ChatWindowStatus.DECLINED, null);
    }

    /** CHT-5: the expiry is heard before the request. Read-only at once; the request fills in the window end. */
    public static ReservationChatWindow expiredBeforeRequested(ReservationId reservationId, CustomerId customerId,
            ProviderId providerId) {
        return new ReservationChatWindow(reservationId, customerId, providerId, ChatWindowStatus.EXPIRED, null);
    }

    /**
     * Rebuilds a window from what was stored. Only the shape is checked: a chat that is still open has the end of
     * its Protection Window. No rule is run again.
     */
    public static ReservationChatWindow restore(ReservationId reservationId, CustomerId customerId,
            ProviderId providerId, ChatWindowStatus status, Instant protectionWindowEnd) {
        ReservationChatWindow window = new ReservationChatWindow(reservationId, customerId, providerId, status,
                protectionWindowEnd);
        if (status == ChatWindowStatus.OPEN && protectionWindowEnd == null) {
            throw new IllegalArgumentException("A stored chat window that is open needs its Protection Window end");
        }
        return window;
    }

    /** The request tells when the Protection Window ends, whatever the window already heard (CHT-2). */
    public ReservationChatWindow withProtectionWindowEnd(Instant protectionWindowEnd) {
        return new ReservationChatWindow(reservationId, customerId, providerId, status,
                Objects.requireNonNull(protectionWindowEnd, "protectionWindowEnd"));
    }

    /** CHT-5: the customer withdrew the reservation, so its chat is read-only at once. */
    public ReservationChatWindow withdrawn() {
        return endedAs(ChatWindowStatus.WITHDRAWN);
    }

    /** CHT-5: the provider declined the reservation, so its chat is read-only at once. */
    public ReservationChatWindow declined() {
        return endedAs(ChatWindowStatus.DECLINED);
    }

    /** CHT-5: the provider missed the acceptance deadline, so the chat is read-only at once. */
    public ReservationChatWindow expired() {
        return endedAs(ChatWindowStatus.EXPIRED);
    }

    /**
     * CHT-5: the reservation was cancelled at {@code cancelledAt}. Before its Check-in Period starts ({@code
     * checkInStart}), the chat is read-only at once. From the moment the Check-in Period starts, nothing changes: the
     * chat stays open until the Protection Window ends.
     */
    public ReservationChatWindow cancelled(Instant cancelledAt, Instant checkInStart) {
        return cancelledAt.isBefore(checkInStart) ? endedAs(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN) : this;
    }

    private ReservationChatWindow endedAs(ChatWindowStatus ended) {
        if (status != ChatWindowStatus.OPEN) {
            return this;
        }
        return new ReservationChatWindow(reservationId, customerId, providerId, ended, protectionWindowEnd);
    }

    /**
     * CHT-2, CHT-5: whether the chat can be written to at {@code now}. A chat that ended with its reservation is
     * read-only whatever the time; any other is open until its Protection Window ends, and that moment itself is
     * already read-only.
     */
    public ChatAccess accessAt(Instant now) {
        return status.readOnlyReason().map(ChatAccess::readOnly).orElseGet(() -> now.isBefore(protectionWindowEnd)
                ? ChatAccess.openUntil(protectionWindowEnd)
                : ChatAccess.readOnly(ChatReadOnlyReason.PROTECTION_WINDOW_ENDED));
    }

    public ReservationId reservationId() {
        return reservationId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public ChatWindowStatus status() {
        return status;
    }

    /** When the Protection Window ends. Empty only for a chat that ended before its request was heard of. */
    public Optional<Instant> protectionWindowEnd() {
        return Optional.ofNullable(protectionWindowEnd);
    }
}
