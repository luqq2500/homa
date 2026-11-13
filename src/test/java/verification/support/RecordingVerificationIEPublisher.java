package verification.support;

import ie.port.VerificationIE;
import java.util.ArrayList;
import java.util.List;
import verification.usecase.port.out.VerificationIEPublisher;

public final class RecordingVerificationIEPublisher implements VerificationIEPublisher {

    private final List<VerificationIE> published = new ArrayList<>();

    @Override
    public void publish(VerificationIE event) {
        published.add(event);
    }

    public List<VerificationIE> published() {
        return List.copyOf(published);
    }

    /** The published events of one type, in order. */
    public <E extends VerificationIE> List<E> published(Class<E> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
