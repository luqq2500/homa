package policy.usecase.port.out;

import ie.port.PolicyIE;

public interface PolicyIEPublisher {

    void publish(PolicyIE event);
}
