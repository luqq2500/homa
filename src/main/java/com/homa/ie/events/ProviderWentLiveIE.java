package com.homa.ie.events;

import com.homa.ie.port.ProviderIE;
import java.time.Instant;

/**
 * A verified provider with at least one active helper went live (PRV-1, PRV-2). Listing shows it, Reservation can
 * book it, Payment opens its accounts.
 */
public record ProviderWentLiveIE(String eventId, String providerId, String businessRegistrationNo, Instant occurredAt)
        implements ProviderIE {
}
