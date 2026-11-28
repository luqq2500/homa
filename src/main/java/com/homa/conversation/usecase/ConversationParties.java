package com.homa.conversation.usecase;

import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.ConversationId;
import com.homa.conversation.domain.PartyRole;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.usecase.exception.ConversationNotFoundException;
import com.homa.conversation.usecase.exception.NotConversationPartyException;
import com.homa.conversation.usecase.exception.ProviderOwnerNotYetKnownException;
import com.homa.conversation.usecase.exception.ReservationChatWindowNotFoundException;
import com.homa.conversation.usecase.port.out.ConversationRepository;
import com.homa.conversation.usecase.port.out.ProviderOwners;
import com.homa.conversation.usecase.port.out.ReservationChatWindowRepository;
import java.util.Optional;

/**
 * Who may act in a conversation, and what a reservation's chat is judged by. Application policy shared by the use
 * cases that act on an existing conversation. The customer acts by their customerId. The provider acts through its
 * owner, as {@code ProviderOpenedIE} named them (PRV-1); rostered members, who act for the provider on the
 * reservations they are rostered on (PRV-11), come in slice 4, when Conversation has the roster.
 */
final class ConversationParties {

    private ConversationParties() {
    }

    static Conversation load(ConversationRepository conversations, String conversationId) {
        return conversations.findById(new ConversationId(conversationId))
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    /**
     * The party the actor is in this conversation. The customer is checked first; anyone else must be the person who
     * speaks for the provider. While the provider's owner is unknown, nobody is taken for them.
     */
    static PartyRole roleOf(Conversation conversation, ProviderOwners owners, String actorId) {
        if (conversation.customerId().value().equals(actorId)) {
            return PartyRole.CUSTOMER;
        }
        String owner = owners.ownerOf(conversation.providerId())
                .orElseThrow(() -> new ProviderOwnerNotYetKnownException(conversation.providerId().value()));
        if (owner.equals(actorId)) {
            return PartyRole.PROVIDER;
        }
        throw new NotConversationPartyException(actorId, conversation.id().value());
    }

    /** The window of the reservation a chat belongs to; empty for an Enquiry, which no reservation judges. */
    static Optional<ReservationChatWindow> windowOf(Conversation conversation,
            ReservationChatWindowRepository windows) {
        return conversation.reservationId().map(reservationId -> windows.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationChatWindowNotFoundException(reservationId.value())));
    }
}
