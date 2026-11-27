package com.homa.provider.usecase;

import com.homa.ie.port.ProviderIE;
import com.homa.provider.usecase.port.out.ProviderIEPublisher;
import java.util.ArrayList;
import java.util.List;

/** Records every published event, in order. */
public final class RecordingProviderIEPublisher implements ProviderIEPublisher {

    private final List<ProviderIE> published = new ArrayList<>();

    @Override
    public void publish(ProviderIE event) {
        published.add(event);
    }

    public List<ProviderIE> published() {
        return List.copyOf(published);
    }

    /** The one event published, of the given type; fails if there was any other number of events. */
    public <E extends ProviderIE> E single(Class<E> type) {
        if (published.size() != 1) {
            throw new AssertionError("Expected one published event, got " + published);
        }
        return type.cast(published.get(0));
    }
}
