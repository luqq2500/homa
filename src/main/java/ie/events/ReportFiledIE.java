package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.List;
import kernel.Money;

/** The customer or the provider filed their report. */
public record ReportFiledIE(String reservationId, String customerId, String providerId, String party, String type, String reason, List<String> evidence, Money atStake, Instant occurredAt) implements ReservationIE {

    public ReportFiledIE {
        evidence = List.copyOf(evidence);
    }
}
