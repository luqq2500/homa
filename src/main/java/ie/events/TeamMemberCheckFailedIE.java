package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** A person's check failed, so nothing was added; the provider is told which and can start again. */
public record TeamMemberCheckFailedIE(String memberId, String businessId, String reason, Instant occurredAt) implements VerificationIE {}
