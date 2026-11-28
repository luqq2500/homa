package com.homa.conversation.usecase;

import com.homa.conversation.domain.ChatAccess;
import com.homa.conversation.domain.ChatReadOnlyReason;
import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.Message;
import com.homa.conversation.domain.PartyRole;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.ReservationId;
import com.homa.conversation.usecase.dto.ViewConversationRequest;
import com.homa.conversation.usecase.dto.ViewConversationResponse;
import com.homa.conversation.usecase.dto.ViewConversationResponse.MessageView;
import com.homa.conversation.usecase.port.in.ConversationViewer;
import com.homa.conversation.usecase.port.out.ConversationRepository;
import com.homa.conversation.usecase.port.out.ProviderOwners;
import com.homa.conversation.usecase.port.out.ReservationChatWindowRepository;
import com.homa.conversation.usecase.port.out.TimeSource;
import java.util.Optional;

/**
 * CHT-5: a party reads the chat, open or read-only, with whether they can write now and why not. Only a party of the
 * conversation may read it.
 *
 * <p>Hook, not built: Support reading a reservation's chat as evidence for a report (CHT-4, slice 5) is another
 * viewer of this same response, added with Support's role; the rule for who may read is the only thing it changes.
 */
public final class ViewConversation implements ConversationViewer {

    private final ConversationRepository conversations;
    private final ReservationChatWindowRepository windows;
    private final ProviderOwners providers;
    private final TimeSource time;

    public ViewConversation(ConversationRepository conversations, ReservationChatWindowRepository windows,
            ProviderOwners providers, TimeSource time) {
        this.conversations = conversations;
        this.windows = windows;
        this.providers = providers;
        this.time = time;
    }

    @Override
    public ViewConversationResponse view(ViewConversationRequest request) {
        Conversation conversation = ConversationParties.load(conversations, request.conversationId());
        PartyRole viewer = ConversationParties.roleOf(conversation, providers, request.viewerId());
        Optional<ReservationChatWindow> window = ConversationParties.windowOf(conversation, windows);
        ChatAccess access = conversation.accessAt(window, time.now());

        return new ViewConversationResponse(
                conversation.id().value(),
                conversation.kind().name(),
                conversation.customerId().value(),
                conversation.providerId().value(),
                conversation.reservationId().map(ReservationId::value).orElse(null),
                viewer.name(),
                access.isOpen(),
                access.readOnlyReason().map(ChatReadOnlyReason::name).orElse(null),
                access.openUntil().orElse(null),
                conversation.messages().stream().map(ViewConversation::view).toList());
    }

    private static MessageView view(Message message) {
        return new MessageView(message.id().value(), message.author().name(), message.text(), message.sentAt());
    }
}
