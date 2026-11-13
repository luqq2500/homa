package policy.support;

import ie.port.PolicyIE;
import java.util.ArrayList;
import java.util.List;
import policy.usecase.port.out.PolicyIEPublisher;

public final class RecordingPolicyIEPublisher implements PolicyIEPublisher {

    private final List<PolicyIE> published = new ArrayList<>();

    @Override
    public void publish(PolicyIE event) {
        published.add(event);
    }

    public List<PolicyIE> published() {
        return List.copyOf(published);
    }
}
