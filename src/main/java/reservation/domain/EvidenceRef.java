package reservation.domain;

/** A reference to a stored photo or video, used only for disputes. */
public record EvidenceRef(String reference) {

    public EvidenceRef {
        reference = ReservationText.require(reference, "An evidence reference");
    }

    public static EvidenceRef of(String reference) {
        return new EvidenceRef(reference);
    }
}
