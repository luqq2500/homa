package com.homa.conversation.usecase.port.out;

import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.ConversationId;
import com.homa.conversation.domain.ReservationId;
import java.util.Optional;

/**
 * Where conversations are kept. Infra rebuilds each one with {@link Conversation#restore} and may add only the
 * messages that are new, since a message, once sent, never changes. A reservation has one chat, so infra backs
 * {@link #findByReservationId} with a unique index on the reservation.
 */
public interface ConversationRepository {

    Optional<Conversation> findById(ConversationId id);

    /** The chat of this reservation. Empty until the reservation's request has been heard of. */
    Optional<Conversation> findByReservationId(ReservationId reservationId);

    /** Adds a new conversation or stores the messages added to an existing one. */
    void save(Conversation conversation);
}
