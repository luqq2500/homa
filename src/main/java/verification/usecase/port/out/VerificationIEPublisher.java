package verification.usecase.port.out;

import ie.port.VerificationIE;

public interface VerificationIEPublisher {

    void publish(VerificationIE event);
}
