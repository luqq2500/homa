package com.homa.conversation.usecase.exception;

/**
 * Conversation doesn't know the provider's owner yet: {@code ProviderOpenedIE} hasn't reached it. Nothing is
 * changed. The owner is on their way, so the caller can try again shortly; infra maps this to "retry later", not to a
 * refusal. The provider's side is never handed to whoever asks first.
 */
public final class ProviderOwnerNotYetKnownException extends ConversationUsecaseException {

    private static final long serialVersionUID = 1L;

    public ProviderOwnerNotYetKnownException(String providerId) {
        super("The owner of provider " + providerId + " isn't known to Conversation yet; try again shortly");
    }
}
