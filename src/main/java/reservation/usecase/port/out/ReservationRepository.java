package reservation.usecase.port.out;

import java.util.List;
import java.util.Optional;
import reservation.domain.Reservation;

public interface ReservationRepository {

    void save(Reservation reservation);

    Optional<Reservation> find(String id);

    List<Reservation> all();

    List<Reservation> ofProvider(String providerId);
}
