package com.homa.rating.domain;

import com.homa.rating.domain.exception.IncidentNotCountedException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The dated incidents of one party, and the counts reliability shows from them (RTG-10): over the last 12 months,
 * filtered by which causes count. Customers see a provider's cancellations, no-rosters, no-shows and members who left
 * during a job; providers see a customer's fee-paying cancellations and no-shows. Free cancellations and withdrawals
 * never become incidents.
 *
 * <p>The same thing on the same reservation is logged once, so an event handled twice can't count twice. Whoever
 * saves a record first moves its {@code version} on, and a save from an older version is refused.
 *
 * <p>Retention (DAT-4): an incident is kept until {@link Incident#stopsCountingAt()}, the date it leaves the count.
 * A purge for it is a later slice.
 */
public final class ReliabilityRecord {

    private final Party party;
    private final List<Incident> incidents = new ArrayList<>();
    private final long version;

    private ReliabilityRecord(Party party, long version) {
        this.party = Objects.requireNonNull(party, "party");
        this.version = version;
    }

    /** A party with nothing logged yet. */
    public static ReliabilityRecord openFor(Party party) {
        return new ReliabilityRecord(party, 0);
    }

    /**
     * Rebuilds a record from what was stored, with the {@code version} it was saved at. Only the shape is checked:
     * every incident is one the party's side can have. Whether an incident is still inside 12 months isn't, since
     * the purge removes old ones later.
     */
    public static ReliabilityRecord restore(Party party, List<Incident> incidents, long version) {
        ReliabilityRecord record = new ReliabilityRecord(party, version);
        for (Incident incident : incidents) {
            if (!incident.kind().concerns(party.role())) {
                throw new IllegalArgumentException("A stored reliability record needs incidents its party can have, "
                        + "not " + incident.kind() + " for a " + party.role());
            }
            record.incidents.add(incident);
        }
        return record;
    }

    /**
     * A reservation was cancelled: logs what it adds to this party's reliability, if anything counts against this
     * party (RTG-10). Returns true when an incident was logged, false when nothing counts or it was logged already.
     */
    public boolean recordCancellation(Cancellation cancellation) {
        Optional<IncidentKind> kind = cancellation.countsAgainst(party.role());
        return kind.isPresent()
                && add(Incident.of(kind.get(), cancellation.reservationId(), cancellation.cancelledAt()));
    }

    /**
     * A roster member left during the job (RPT-8): counts against a provider. Reservation's roster-member-left event
     * will call this in a later slice. Returns false when that member's leaving was logged already.
     *
     * @throws IncidentNotCountedException when this party is a customer, whose reliability has no members who left
     */
    public boolean recordMemberLeft(ReservationId reservationId, String memberId, Instant leftAt) {
        return add(Incident.memberLeft(reservationId, memberId, leftAt));
    }

    private boolean add(Incident incident) {
        if (!incident.kind().concerns(party.role())) {
            throw new IncidentNotCountedException(party, incident.kind());
        }
        if (incidents.stream().anyMatch(incident::isSameAs)) {
            return false;
        }
        incidents.add(incident);
        return true;
    }

    /** The counts over the last 12 months as of {@code now} (RTG-10). */
    public ReliabilityCounts countsAt(Instant now) {
        return new ReliabilityCounts(countOf(IncidentKind.CANCELLATION, now), countOf(IncidentKind.NO_ROSTER, now),
                countOf(IncidentKind.NO_SHOW, now), countOf(IncidentKind.MEMBER_LEFT, now));
    }

    private int countOf(IncidentKind kind, Instant now) {
        return (int) incidents.stream().filter(incident -> incident.kind() == kind && incident.countsAt(now)).count();
    }

    public Party party() {
        return party;
    }

    /** Every incident logged, counting or not yet purged, oldest logged first: what the store keeps. */
    public List<Incident> incidents() {
        return List.copyOf(incidents);
    }

    /**
     * The version the record was loaded at, 0 for one not saved yet. Whoever saves it first moves it on, and a save
     * from an older version is refused as a conflict.
     */
    public long version() {
        return version;
    }
}
