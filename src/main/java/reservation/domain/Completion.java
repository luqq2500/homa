package reservation.domain;

import java.time.Instant;
import java.util.List;
import reservation.domain.exception.EvidenceMissingException;

/** The provider's mark that the job is completed, with the photo or video evidence it requires. */
public record Completion(List<String> evidence, Instant submittedAt) {

    public Completion {
        if (evidence == null || evidence.isEmpty()) {
            throw new EvidenceMissingException();
        }
        evidence = List.copyOf(evidence);
    }
}
