package com.homa.conversation.usecase;

import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.Message;
import com.homa.conversation.domain.PartyRole;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.usecase.dto.SendMessageRequest;
import com.homa.conversation.usecase.dto.SendMessageResponse;
import com.homa.conversation.usecase.port.in.MessageSender;
import com.homa.conversation.usecase.port.out.ConversationRepository;
import com.homa.conversation.usecase.port.out.ProviderOwners;
import com.homa.conversation.usecase.port.out.ReservationChatWindowRepository;
import com.homa.conversation.usecase.port.out.TimeSource;
import java.util.Optional;

/**
 * CHT-2, CHT-5: the customer or the provider writes a message. Only a party of the conversation may, and the domain
 * decides whether the chat is open at this moment from the reservation's window and the clock, so nothing here
 * compares a time. The message is kept as written (CHT-3). Publishes nothing.
 */
public final class PartySendMessage implements MessageSender {

    private final ConversationRepository conversations;
    private final ReservationChatWindowRepository windows;
    private final ProviderOwners providers;
    private final TimeSource time;

    public PartySendMessage(ConversationRepository conversations, ReservationChatWindowRepository windows,
            ProviderOwners providers, TimeSource time) {
        this.conversations = conversations;
        this.windows = windows;
        this.providers = providers;
        this.time = time;
    }

    @Override
    public SendMessageResponse send(SendMessageRequest request) {
        Conversation conversation = ConversationParties.load(conversations, request.conversationId());
        PartyRole author = ConversationParties.roleOf(conversation, providers, request.senderId());
        Optional<ReservationChatWindow> window = ConversationParties.windowOf(conversation, windows);

        Message message = conversation.send(author, request.text(), window, time.now());
        conversations.save(conversation);

        return new SendMessageResponse(conversation.id().value(), message.id().value(), author.name(),
                message.text(), message.sentAt());
    }
}
