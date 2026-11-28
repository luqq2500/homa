package com.homa.conversation.usecase.port.out;

import java.time.Instant;

/** The Conversation context's clock. */
public interface TimeSource {

    Instant now();
}
