package reservation.usecase;

import ie.events.ProviderFinedIE;
import ie.events.ReservationAcceptedIE;
import ie.events.ReservationCompletedIE;
import ie.events.ReservationEndedIE;
import ie.events.ReservationSettledIE;
import java.time.Instant;
import java.util.Set;
import kernel.TimeRange;
import reservation.domain.Ending;
import reservation.domain.JobTeam;
import reservation.domain.Reservation;
import reservation.domain.ReservationState;
import reservation.domain.ReservationStatus;
import reservation.domain.Settlement;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

/** Tells the other contexts what happened to a reservation: accepted, ended or completed, settled, fined. */
final class ReservationAnnouncer {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;

    ReservationAnnouncer(ReservationRepository reservations, ReservationIEPublisher publisher) {
        this.reservations = reservations;
        this.publisher = publisher;
    }

    /**
     * The request was accepted: it now holds the provider's time, and every other open request of
     * theirs that clashes with it, or with the buffer around it, is declined at once.
     */
    void accepted(Reservation accepted, ProviderScheduleReader schedules, Instant now) {
        TimeRange job = accepted.details().bookedTime();
        publisher.publish(new ReservationAcceptedIE(accepted.id(), accepted.customerId(), accepted.providerId(),
                job.start(), job.end(), accepted.state().terms().usesInstantBook(), now));
        for (Reservation other : reservations.ofProvider(accepted.providerId())) {
            if (other.declineIfClashingWith(job, schedules.openTimeOf(accepted.providerId()).buffer())) {
                reservations.save(other);
                ended(other, now);
            }
        }
    }

    /** The reservation reached an end: it is announced, its money is split, and any fine is raised. */
    void ended(Reservation reservation, Instant now) {
        ReservationState state = reservation.state();
        if (state.status() == ReservationStatus.COMPLETED) {
            publisher.publish(new ReservationCompletedIE(state.id(), state.customerId(), reservation.providerId(),
                    teamOf(state), !state.settlement().providerReceives().isZero(), now));
        } else {
            publisher.publish(new ReservationEndedIE(state.id(), state.customerId(), reservation.providerId(),
                    state.status().name(), state.ending().name(), state.acceptedAt() != null,
                    state.details().start(), state.details().end(), now));
        }
        if (moneyMovesNow(state)) {
            settled(reservation, now);
        }
        reservation.fine().ifPresent(fine -> publisher.publish(new ProviderFinedIE(state.id(),
                reservation.providerId(), fine.amount(), fine.reason().name(), now)));
    }

    /** A failed payment charged nothing, and a customer no-show fee is held until its report window ends. */
    private static boolean moneyMovesNow(ReservationState state) {
        boolean held = state.ending() == Ending.CUSTOMER_NO_SHOW && !state.customerNoShowFeeReleased();
        return state.ending() != Ending.PAYMENT_FAILED && !held;
    }

    void settled(Reservation reservation, Instant now) {
        Settlement settlement = reservation.settlement().orElseThrow();
        publisher.publish(new ReservationSettledIE(reservation.id(), reservation.providerId(),
                settlement.providerReceives(), settlement.kind().name(), now));
    }

    private static Set<String> teamOf(ReservationState state) {
        return state.jobTeam() == null ? Set.of() : new JobTeam(state.jobTeam().people()).memberIds();
    }
}
