package com.homa.conversation.domain;

import com.homa.conversation.domain.exception.ChatReadOnlyException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A chat between a customer and a provider, and the messages they have sent: either an Enquiry the customer starts
 * before reserving, or the chat of one reservation (CHT-1). Only a customer can start a chat. An Enquiry begins with
 * the customer's question, so the provider can only ever answer a customer who has contacted them; a reservation's
 * chat begins when the customer's reservation is Requested.
 *
 * <p>Whether a chat can be written to is worked out when it is asked, from the clock and, for a reservation's chat,
 * the reservation's {@link ReservationChatWindow} (CHT-2, CHT-5). An Enquiry stays open: a customer who wants to ask
 * something new after a reservation's chat has become read-only starts a new Enquiry. Nothing is stored that would
 * have to flip when a Protection Window ends.
 *
 * <p>Homa's off-platform reminder (CHT-3) is copy every chat shows in the apps, not something a conversation knows or
 * enforces, and messages are kept as they were written. Support's reading of a reservation's chat as evidence (CHT-4)
 * does not change a conversation either: it is a read, added with Support's reports in slice 5. Retention (DAT-5,
 * slice 9) adds when a chat is due for deletion, from its last message and its reservation's Protection Window end.
 */
public final class Conversation {

    private final ConversationId id;
    private final ConversationKind kind;
    private final CustomerId customerId;
    private final ProviderId providerId;
    private final ReservationId reservationId;
    private final Instant startedAt;
    private final List<Message> messages;

    private Conversation(ConversationId id, ConversationKind kind, CustomerId customerId, ProviderId providerId,
            ReservationId reservationId, Instant startedAt, List<Message> messages) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.reservationId = reservationId;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.messages = new ArrayList<>(messages);
    }

    /**
     * CHT-1: a customer in good standing starts an Enquiry with a provider by asking their question; a suspended
     * customer can't (CUS-5). Customers can start as many Enquiries as they like: a new question goes in a new one
     * (CHT-5).
     */
    public static Conversation startEnquiry(CustomerStanding customer, ProviderId providerId, String question,
            Instant now) {
        customer.requireCanStartEnquiryAt(now);
        Message opening = Message.sentBy(PartyRole.CUSTOMER, question, now);
        return new Conversation(ConversationId.generate(), ConversationKind.ENQUIRY, customer.customerId(),
                providerId, null, now, List.of(opening));
    }

    /**
     * CHT-1, CHT-2: the customer's reservation is Requested, which starts its chat between them and the provider. It
     * has no messages yet, and either party may write first.
     */
    public static Conversation openReservationChat(ReservationChatWindow window, Instant now) {
        return new Conversation(ConversationId.generate(), ConversationKind.RESERVATION, window.customerId(),
                window.providerId(), window.reservationId(), now, List.of());
    }

    /**
     * Rebuilds a conversation from what was stored, with its messages in the order they were sent. Only the shape is
     * checked: a reservation's chat names its reservation and an Enquiry doesn't, and an Enquiry begins with its
     * customer's message. No rule is run again.
     */
    public static Conversation restore(ConversationId id, ConversationKind kind, CustomerId customerId,
            ProviderId providerId, ReservationId reservationId, Instant startedAt, List<Message> messages) {
        requireReservationExactlyForAReservationsChat(kind, reservationId);
        requireEnquiryToBeginWithItsCustomer(kind, messages);
        return new Conversation(id, kind, customerId, providerId, reservationId, startedAt, messages);
    }

    private static void requireReservationExactlyForAReservationsChat(ConversationKind kind,
            ReservationId reservationId) {
        requireShape((kind == ConversationKind.RESERVATION) == (reservationId != null),
                "a reservation exactly when it is a reservation's chat");
    }

    private static void requireEnquiryToBeginWithItsCustomer(ConversationKind kind, List<Message> messages) {
        requireShape(kind != ConversationKind.ENQUIRY || startsWithCustomer(messages),
                "an Enquiry that begins with its customer's message");
    }

    private static boolean startsWithCustomer(List<Message> messages) {
        return !messages.isEmpty() && messages.getFirst().author() == PartyRole.CUSTOMER;
    }

    private static void requireShape(boolean fits, String what) {
        if (!fits) {
            throw new IllegalArgumentException("A stored conversation needs " + what);
        }
    }

    /**
     * CHT-2, CHT-5: whether the chat can be written to at {@code now}. An Enquiry always can. A reservation's chat is
     * judged by its own reservation's {@code window}, which must be passed; it is ignored for an Enquiry.
     */
    public ChatAccess accessAt(Optional<ReservationChatWindow> window, Instant now) {
        if (kind == ConversationKind.ENQUIRY) {
            return ChatAccess.open();
        }
        return window.filter(own -> own.reservationId().equals(reservationId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "A reservation's chat is judged by the window of its own reservation"))
                .accessAt(now);
    }

    /**
     * CHT-1, CHT-2, CHT-5: a party writes a message at {@code now}. Either party can write in an open chat, and
     * nobody in a read-only one. The caller decides who {@code author} is; the chat keeps the text as written.
     *
     * @return the message, now the last of the conversation
     * @throws ChatReadOnlyException when the chat can only be read
     */
    public Message send(PartyRole author, String text, Optional<ReservationChatWindow> window, Instant now) {
        accessAt(window, now).readOnlyReason().ifPresent(reason -> {
            throw new ChatReadOnlyException(reason);
        });
        Message message = Message.sentBy(author, text, now);
        messages.add(message);
        return message;
    }

    public ConversationId id() {
        return id;
    }

    public ConversationKind kind() {
        return kind;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    /** The reservation this is the chat of. Empty for an Enquiry. */
    public Optional<ReservationId> reservationId() {
        return Optional.ofNullable(reservationId);
    }

    public Instant startedAt() {
        return startedAt;
    }

    /** Every message, in the order they were sent. */
    public List<Message> messages() {
        return List.copyOf(messages);
    }
}
