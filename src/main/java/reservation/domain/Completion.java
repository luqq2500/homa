package reservation.domain;

import java.time.Instant;
import java.util.List;

/** When the provider marked the job completed, and the photo or video evidence they gave. */
public record Completion(Instant at, List<EvidenceRef> evidence) {

    public Completion {
        evidence = List.copyOf(evidence);
    }
}
