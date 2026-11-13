package availability.usecase.port.out;

import ie.port.AvailabilityIE;

public interface AvailabilityIEPublisher {

    void publish(AvailabilityIE event);
}
