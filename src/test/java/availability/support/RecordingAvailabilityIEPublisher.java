package availability.support;

import ie.port.AvailabilityIE;
import java.util.ArrayList;
import java.util.List;
import availability.usecase.port.out.AvailabilityIEPublisher;

public final class RecordingAvailabilityIEPublisher implements AvailabilityIEPublisher {

    private final List<AvailabilityIE> published = new ArrayList<>();

    @Override
    public void publish(AvailabilityIE event) {
        published.add(event);
    }

    public List<AvailabilityIE> published() {
        return List.copyOf(published);
    }

    /** The published events of one type, in order. */
    public <E extends AvailabilityIE> List<E> published(Class<E> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
