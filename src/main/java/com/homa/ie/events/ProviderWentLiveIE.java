package com.homa.ie.events;

import com.homa.ie.port.ProviderIE;
import java.time.Instant;

/**
 * A provider with a verified owner, a verified payout account and at least one active helper went live (PRV-1,
 * PRV-2). Listing shows it, Reservation can book it, Payment opens its accounts. PAY-9 and DAT-10 match a rejoin on
 * the owner's personId or the business registration number.
 *
 * @param businessRegistrationNo the registered business's number for a Business provider (PRV-7); null otherwise
 */
public record ProviderWentLiveIE(
        String eventId, String providerId, String ownerPersonId, String businessRegistrationNo, Instant occurredAt)
        implements ProviderIE {
}
