package resolution.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import kernel.Money;
import resolution.domain.exception.CaseAlreadyDecidedException;
import resolution.domain.exception.DecisionReasonRequiredException;
import resolution.domain.exception.InvalidDecisionAmountException;
import resolution.domain.exception.ResolutionDetailRequiredException;

/**
 * One case in the founders' report queue: a reported, challenged or unfinished reservation, with its
 * evidence and the amount at stake. The founders decide it once, with a short reason.
 */
public final class Case {

    private final String id;
    private final String reservationId;
    private final CaseSource source;
    private final Money atStake;
    private final Instant openedAt;
    private final List<EvidenceRef> evidence;
    private CaseStatus status = CaseStatus.OPEN;
    private Decision decision;
    private String reason;
    private Instant decidedAt;

    private Case(String reservationId, CaseSource source, List<EvidenceRef> evidence, Money atStake, Instant openedAt) {
        this.id = new CaseId().generate();
        this.reservationId = reservationId;
        this.source = source;
        this.evidence = new ArrayList<>(evidence);
        this.atStake = atStake;
        this.openedAt = openedAt;
    }

    /**
     * Opens a case.
     *
     * @param atStake what the provider could receive: the provider price for a job, or the customer no-show
     *     fee for a challenged no-show
     */
    public static Case open(String reservationId, CaseSource source, List<EvidenceRef> evidence, Money atStake, Instant now) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new ResolutionDetailRequiredException("A reservation id");
        }
        return new Case(reservationId, Objects.requireNonNull(source), evidence, Objects.requireNonNull(atStake), Objects.requireNonNull(now));
    }

    /** Adds evidence while the case is open, for example when the other party also reports. */
    public void addEvidence(List<EvidenceRef> more) {
        requireOpen();
        evidence.addAll(more);
    }

    public void decide(Decision chosen, String why, Instant now) {
        requireOpen();
        if (why == null || why.isBlank()) {
            throw new DecisionReasonRequiredException();
        }
        chosen.providerReceives().filter(amount -> amount.isNegative() || amount.isGreaterThan(atStake)).ifPresent(amount -> {
            throw new InvalidDecisionAmountException(amount, atStake);
        });
        decision = chosen;
        reason = why.strip();
        decidedAt = Objects.requireNonNull(now);
        status = CaseStatus.DECIDED;
    }

    public boolean isOpen() {
        return status == CaseStatus.OPEN;
    }

    public String id() {
        return id;
    }

    public String reservationId() {
        return reservationId;
    }

    public CaseSource source() {
        return source;
    }

    public Money atStake() {
        return atStake;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public List<EvidenceRef> evidence() {
        return List.copyOf(evidence);
    }

    public CaseStatus status() {
        return status;
    }

    public Optional<Decision> decision() {
        return Optional.ofNullable(decision);
    }

    /**
     * What the provider receives under the decision: what was at stake for a release, nothing for a full
     * refund, or the decided amount for a partial refund. Empty until the case is decided.
     */
    public Optional<Money> providerReceives() {
        return decision().map(decided -> switch (decided.outcome()) {
            case RELEASE -> atStake;
            case FULL_REFUND -> Money.ZERO;
            case PARTIAL_REFUND -> decided.providerReceives().orElseThrow();
        });
    }

    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    public Optional<Instant> decidedAt() {
        return Optional.ofNullable(decidedAt);
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new CaseAlreadyDecidedException(id);
        }
    }
}
