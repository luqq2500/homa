package offering.usecase.port.out;

import ie.port.OfferingIE;

public interface OfferingIEPublisher {

    void publish(OfferingIE event);
}
