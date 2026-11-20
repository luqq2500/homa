package com.homa.app.outbox.sample.conversation.usecase.port.out;

/** The second sample consumer's read model: who may chat. */
public interface Members {

    void admit(String customerId);
}
