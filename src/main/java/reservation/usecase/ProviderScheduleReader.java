package reservation.usecase;

import java.util.List;
import kernel.TimeRange;
import reservation.domain.ProviderOpenTime;
import reservation.domain.ProviderSchedule;
import reservation.domain.ProviderStanding;
import reservation.domain.Reservation;
import reservation.usecase.port.out.ProviderOpenTimeRepository;
import reservation.usecase.port.out.ProviderStandingRepository;
import reservation.usecase.port.out.ReservationRepository;

/** Reads when a provider can take a job: their open time, their buffer and the jobs they have accepted. */
final class ProviderScheduleReader {

    private final ProviderOpenTimeRepository openTimes;
    private final ProviderStandingRepository standings;
    private final ReservationRepository reservations;

    ProviderScheduleReader(ProviderOpenTimeRepository openTimes, ProviderStandingRepository standings,
                           ReservationRepository reservations) {
        this.openTimes = openTimes;
        this.standings = standings;
        this.reservations = reservations;
    }

    ProviderOpenTime openTimeOf(String providerId) {
        return openTimes.find(providerId).orElseGet(() -> ProviderOpenTime.none(providerId));
    }

    ProviderStanding standingOf(String providerId) {
        return standings.find(providerId).orElseGet(() -> ProviderStanding.unverified(providerId));
    }

    /** The provider's schedule as another booking sees it: every accepted job except the booking itself. */
    ProviderSchedule scheduleOf(String providerId, String exceptReservationId) {
        List<TimeRange> acceptedJobs = reservations.ofProvider(providerId).stream()
                .filter(Reservation::holdsProvidersTime)
                .filter(reservation -> !reservation.id().equals(exceptReservationId))
                .map(reservation -> reservation.details().bookedTime())
                .toList();
        return openTimeOf(providerId).scheduleWith(acceptedJobs);
    }
}
