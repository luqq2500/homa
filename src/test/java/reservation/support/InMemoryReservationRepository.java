package reservation.support;

import java.util.List;
import java.util.Optional;
import reservation.domain.Reservation;
import reservation.usecase.port.out.ReservationRepository;
import support.InMemoryStore;

public final class InMemoryReservationRepository extends InMemoryStore<Reservation> implements ReservationRepository {

    public InMemoryReservationRepository() {
        super(Reservation::id);
    }

    @Override
    public List<Reservation> ofProvider(String providerId) {
        return all().stream().filter(reservation -> reservation.providerId().equals(providerId)).toList();
    }
}
