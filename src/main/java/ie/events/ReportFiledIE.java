package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import kernel.Money;

/** The customer or the provider filed a report; Homa reviews it. */
public record ReportFiledIE(
        String reservationId,
        String party,
        String reportType,
        String reason,
        List<String> evidence,
        Money atStake,
        Instant occurredAt) implements ReservationIE {

    public ReportFiledIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(party, "party");
        Objects.requireNonNull(reportType, "reportType");
        Objects.requireNonNull(reason, "reason");
        evidence = List.copyOf(evidence);
        Objects.requireNonNull(atStake, "atStake");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
