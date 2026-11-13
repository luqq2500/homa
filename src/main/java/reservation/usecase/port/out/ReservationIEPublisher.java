package reservation.usecase.port.out;

import ie.port.ReservationIE;

public interface ReservationIEPublisher {

    void publish(ReservationIE event);
}
