package ie.events;

import ie.port.AccountIE;
import java.time.Instant;

/** A customer or business closed their account. */
public record AccountClosedIE(String accountId, String accountKind, Instant occurredAt) implements AccountIE {}
