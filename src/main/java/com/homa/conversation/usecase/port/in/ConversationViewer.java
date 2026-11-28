package com.homa.conversation.usecase.port.in;

import com.homa.conversation.usecase.dto.ViewConversationRequest;
import com.homa.conversation.usecase.dto.ViewConversationResponse;

/**
 * A party reads a chat, open or read-only, with every message in it (CHT-5).
 *
 * <p>Hook, not built: Support reads a reservation's chat as evidence for a report (CHT-4, slice 5). It is a read of
 * the same chat by another actor, so it plugs in here as a viewer who isn't a party, once Support's role reaches the
 * use cases; nothing about a conversation changes for it.
 */
public interface ConversationViewer {

    ViewConversationResponse view(ViewConversationRequest request);
}
