package conversation.domain;

import conversation.domain.exception.ConversationAlreadyLinkedException;
import conversation.domain.exception.ConversationArchivedException;
import conversation.domain.exception.ConversationDetailRequiredException;
import conversation.domain.exception.OnlyCustomerCanStartChatException;
import conversation.domain.exception.ReservationChatCannotBeArchivedException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A chat between a customer and a provider. Every chat belongs to one enquiry or reservation. Only the
 * customer can start it; the provider can reply. When an enquiry is requested, the same chat carries on
 * for the reservation and stays open for its whole life. An enquiry closed without a request has its chat
 * archived: still readable, but closed to new messages.
 */
public final class Conversation {

    private final String id;
    private final String customerId;
    private final String providerId;
    private final List<Message> messages = new ArrayList<>();
    private ChatSubject subject;
    private Instant archivedAt;

    private Conversation(ChatSubject subject, String customerId, String providerId) {
        this.id = new ConversationId().generate();
        this.subject = subject;
        this.customerId = customerId;
        this.providerId = providerId;
    }

    /** Starts a chat with its first message. Only the customer can start a chat. */
    public static Conversation start(
            Party startedBy, ChatSubject subject, String customerId, String providerId, String firstMessage, Instant now) {
        if (startedBy != Party.CUSTOMER) {
            throw new OnlyCustomerCanStartChatException();
        }
        Conversation conversation =
                new Conversation(Objects.requireNonNull(subject), require(customerId, "A customer id"), require(providerId, "A provider id"));
        conversation.messages.add(new Message(Party.CUSTOMER, firstMessage, now));
        return conversation;
    }

    public static Conversation startByCustomer(
            ChatSubject subject, String customerId, String providerId, String firstMessage, Instant now) {
        return start(Party.CUSTOMER, subject, customerId, providerId, firstMessage, now);
    }

    public void post(Party sender, String text, Instant now) {
        if (isArchived()) {
            throw new ConversationArchivedException();
        }
        messages.add(new Message(sender, text, now));
    }

    /** The enquiry was requested: the same chat, with its messages, now belongs to the reservation. */
    public void linkReservation(String reservationId) {
        if (subject.kind() == ChatSubject.Kind.RESERVATION) {
            throw new ConversationAlreadyLinkedException(subject.id());
        }
        subject = ChatSubject.reservation(reservationId);
    }

    /** The enquiry closed without a request. A reservation's chat is never archived. */
    public void archive(Instant now) {
        if (subject.kind() == ChatSubject.Kind.RESERVATION) {
            throw new ReservationChatCannotBeArchivedException();
        }
        if (isArchived()) {
            throw new ConversationArchivedException();
        }
        archivedAt = Objects.requireNonNull(now);
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public Optional<Instant> archivedAt() {
        return Optional.ofNullable(archivedAt);
    }

    public List<Message> messages() {
        return List.copyOf(messages);
    }

    public ChatSubject subject() {
        return subject;
    }

    public String id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public String providerId() {
        return providerId;
    }

    private static String require(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new ConversationDetailRequiredException(what);
        }
        return text;
    }
}
