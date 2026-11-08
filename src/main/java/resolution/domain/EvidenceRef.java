package resolution.domain;

import resolution.domain.exception.ResolutionDetailRequiredException;

/** A reference to stored evidence: a photo, a video, or the check-in record. */
public record EvidenceRef(String reference) {

    public EvidenceRef {
        if (reference == null || reference.isBlank()) {
            throw new ResolutionDetailRequiredException("An evidence reference");
        }
        reference = reference.strip();
    }

    public static EvidenceRef of(String reference) {
        return new EvidenceRef(reference);
    }
}
