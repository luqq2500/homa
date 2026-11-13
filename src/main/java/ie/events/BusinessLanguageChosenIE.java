package ie.events;

import ie.port.AccountIE;
import java.time.Instant;

/** A business chose the language of its provider app. */
public record BusinessLanguageChosenIE(String businessId, String language, Instant occurredAt) implements AccountIE {}
