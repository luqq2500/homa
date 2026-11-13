package reservation.domain;

import java.time.Instant;
import java.util.List;
import reservation.domain.exception.ReasonMissingException;

/** A report filed by the customer or the provider, with a reason, and evidence where available. */
public record Report(ReportingParty party, ReportType type, String reason, List<String> evidence, Instant filedAt) {

    public Report {
        if (reason == null || reason.isBlank()) {
            throw new ReasonMissingException();
        }
        evidence = List.copyOf(evidence);
    }
}
