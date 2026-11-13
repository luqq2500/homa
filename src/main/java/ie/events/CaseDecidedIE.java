package ie.events;

import ie.port.ResolutionIE;
import java.time.Instant;
import kernel.Money;

/** Homa decided a case: what the provider receives, from RM0 up to what was at stake. */
public record CaseDecidedIE(String reservationId, Money providerReceives, String adminId, String reason, Instant occurredAt) implements ResolutionIE {}
