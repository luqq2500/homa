package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** A lapsed team member passed re-verification and is back on the team. */
public record TeamMemberRestoredIE(String memberId, String businessId, String role, String registeredName, String profilePhoto, Instant lastVerifiedAt, Instant occurredAt) implements VerificationIE {}
