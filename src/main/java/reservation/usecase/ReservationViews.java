package reservation.usecase;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import reservation.domain.Ending;
import reservation.domain.JobTeam;
import reservation.domain.Reservation;
import reservation.domain.ReservationState;

/** What a customer or a provider sees of a reservation, read from the reservation. */
record ReservationViews(Reservation reservation, ReservationState booked) {

    static ReservationViews of(Reservation reservation) {
        return new ReservationViews(reservation, reservation.state());
    }

    String ending() {
        return reservation.ending().map(Ending::name).orElse(null);
    }

    Set<String> teamMemberIds() {
        return reservation.jobTeam().map(JobTeam::memberIds).orElse(Set.of());
    }

    Instant disputeWindowEndsAt() {
        return reservation.disputeWindowEndsAt().orElse(null);
    }

    /** The provider sees the access notes from acceptance. */
    String accessNotesForProvider() {
        return Optional.ofNullable(booked.acceptedAt()).map(accepted -> booked.accessNotes()).orElse(null);
    }
}
