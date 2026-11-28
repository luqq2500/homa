package com.homa.conversation.usecase.port.in;

import com.homa.ie.events.ProviderOpenedIE;

/** A provider opened: its owner is the person who speaks for it in its chats (PRV-1). */
public interface ProviderOpenedIEHandler {

    void handle(ProviderOpenedIE event);
}
