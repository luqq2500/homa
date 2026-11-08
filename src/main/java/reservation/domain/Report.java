package reservation.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** A report filed by the customer or the provider, with a reason and any evidence. */
public record Report(Party party, ReportType type, String reason, List<EvidenceRef> evidence, Instant filedAt) {

    public Report {
        Objects.requireNonNull(party);
        Objects.requireNonNull(type);
        reason = ReservationText.require(reason, "A report reason");
        evidence = List.copyOf(evidence);
    }
}
