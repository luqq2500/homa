package com.homa.conversation.usecase.port.out;

import com.homa.conversation.domain.ProviderId;
import java.util.Optional;

/**
 * Who speaks for a provider in its chats: its owner's personId, as {@code ProviderOpenedIE} named them (PRV-1).
 * Application policy, not a domain rule. A provider is known to Conversation exactly when its owner is. Until
 * Provider's roster events reach Conversation (PRV-11, slice 4), the owner is the only person who can, and the
 * rostered members who may speak for the provider on a reservation can be added beside it. Empty until the event
 * has reached Conversation.
 */
public interface ProviderOwners {

    Optional<String> ownerOf(ProviderId providerId);

    /** Records the owner Provider named when the provider opened. */
    void record(ProviderId providerId, String ownerPersonId);
}
