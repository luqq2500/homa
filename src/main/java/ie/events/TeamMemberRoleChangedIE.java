package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** A team member changed between helper and supervisor. */
public record TeamMemberRoleChangedIE(String memberId, String businessId, String role, Instant occurredAt) implements VerificationIE {}
