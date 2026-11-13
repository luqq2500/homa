package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** A team member lapsed or was removed: they can no longer be named for jobs and come off upcoming ones. */
public record TeamMemberLeftIE(String memberId, String businessId, String reason, Instant occurredAt) implements VerificationIE {}
