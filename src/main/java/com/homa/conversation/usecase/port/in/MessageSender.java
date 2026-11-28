package com.homa.conversation.usecase.port.in;

import com.homa.conversation.usecase.dto.SendMessageRequest;
import com.homa.conversation.usecase.dto.SendMessageResponse;

/** CHT-2, CHT-5: the customer or the provider writes a message in a chat that is open. */
public interface MessageSender {

    SendMessageResponse send(SendMessageRequest request);
}
