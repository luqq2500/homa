package offering.support;

import ie.port.OfferingIE;
import java.util.ArrayList;
import java.util.List;
import offering.usecase.port.out.OfferingIEPublisher;

public final class RecordingOfferingIEPublisher implements OfferingIEPublisher {

    private final List<OfferingIE> published = new ArrayList<>();

    @Override
    public void publish(OfferingIE event) {
        published.add(event);
    }

    public List<OfferingIE> published() {
        return List.copyOf(published);
    }

    /** The published events of one type, in order. */
    public <E extends OfferingIE> List<E> published(Class<E> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
