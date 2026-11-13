package resolution.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import kernel.Money;
import resolution.domain.exception.CaseAlreadyDecidedException;
import resolution.domain.exception.CaseNotOpenException;
import resolution.domain.exception.DecisionReasonRequiredException;
import resolution.domain.exception.InvalidDecisionAmountException;
import resolution.domain.exception.ResolutionDetailRequiredException;

/**
 * One case in the admins' report queue: a reported, challenged or unfinished reservation, with its
 * evidence and the amount at stake. The admins decide it once, with a short reason.
 */
public final class Case {

    /** The admins' queue order: cases about safety, damage or loss first, then the oldest first. */
    public static final Comparator<Case> QUEUE_ORDER =
            Comparator.comparing((Case queued) -> !queued.isSafety()).thenComparing(Case::openedAt);

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
    private boolean safety;
    private Instant closedAt;

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

    /** A safety, damage or loss report opened the case or joined it, so it goes to the top of the queue. */
    public void markSafety() {
        requireOpen();
        safety = true;
    }

    public boolean isSafety() {
        return safety;
    }

    /** A flag took the reservation over: the case closes with no decision. */
    public void closeForProviderFlag(Instant now) {
        requireOpen();
        closedAt = Objects.requireNonNull(now);
        status = CaseStatus.CLOSED;
    }

    /** When a flag closed the case; empty unless it is closed. */
    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
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

    /**
     * What the provider must cover under the decision: the amount at stake less what they receive. For a
     * chargeback, it is recovered from the provider's next payouts like a fine. Empty until decided.
     */
    public Optional<Money> providerCovers() {
        return providerReceives().map(atStake::minus);
    }

    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    public Optional<Instant> decidedAt() {
        return Optional.ofNullable(decidedAt);
    }

    private void requireOpen() {
        if (status == CaseStatus.CLOSED) {
            throw new CaseNotOpenException(id);
        }
        if (status == CaseStatus.DECIDED) {
            throw new CaseAlreadyDecidedException(id);
        }
    }
}
