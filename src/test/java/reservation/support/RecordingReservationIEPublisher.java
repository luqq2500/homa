package reservation.support;

import ie.port.ReservationIE;
import java.util.ArrayList;
import java.util.List;
import reservation.usecase.port.out.ReservationIEPublisher;

public final class RecordingReservationIEPublisher implements ReservationIEPublisher {

    private final List<ReservationIE> published = new ArrayList<>();

    @Override
    public void publish(ReservationIE event) {
        published.add(event);
    }

    public List<ReservationIE> published() {
        return List.copyOf(published);
    }

    /** The published events of one type, in order. */
    public <E extends ReservationIE> List<E> published(Class<E> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
