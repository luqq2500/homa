package policy.usecase.port.out;

import ie.port.PolicyIE;

/** Publishes the policy's integration events to the other contexts. */
public interface PolicyIEPublisher {

    void publish(PolicyIE event);
}
