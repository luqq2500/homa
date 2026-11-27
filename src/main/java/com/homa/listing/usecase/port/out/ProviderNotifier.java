package com.homa.listing.usecase.port.out;

import java.time.Duration;

/** Tells a provider about a change Listing made for it. */
public interface ProviderNotifier {

    /** Switching to Request raised the provider's minimum notice to this (OFR-6). */
    void noticeRaised(String providerId, Duration minimumNotice);
}
