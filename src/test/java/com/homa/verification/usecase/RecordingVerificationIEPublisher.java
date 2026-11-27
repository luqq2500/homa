package com.homa.verification.usecase;

import com.homa.ie.port.VerificationIE;
import com.homa.verification.usecase.port.out.VerificationIEPublisher;
import java.util.ArrayList;
import java.util.List;

/** Records every event published, in order. */
public final class RecordingVerificationIEPublisher implements VerificationIEPublisher {

    private final List<VerificationIE> published = new ArrayList<>();

    @Override
    public void publish(VerificationIE event) {
        published.add(event);
    }

    public List<VerificationIE> published() {
        return List.copyOf(published);
    }

    /** Every published event of {@code type}. */
    public <E extends VerificationIE> List<E> published(Class<E> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** The one event published so far, of {@code type}. */
    public <E extends VerificationIE> E single(Class<E> type) {
        List<E> found = published(type);
        if (found.size() != 1 || published.size() != 1) {
            throw new AssertionError("Expected exactly one " + type.getSimpleName() + " but got " + published);
        }
        return found.get(0);
    }

    public void clear() {
        published.clear();
    }
}
