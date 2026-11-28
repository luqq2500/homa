package com.homa.rating.usecase;

import com.homa.ie.port.RatingIE;
import com.homa.rating.usecase.port.out.RatingIEPublisher;
import java.util.ArrayList;
import java.util.List;

/** Test only: every event published, in order. */
public class RecordingRatingIEPublisher implements RatingIEPublisher {

    private final List<RatingIE> published = new ArrayList<>();

    @Override
    public void publish(RatingIE event) {
        published.add(event);
    }

    public List<RatingIE> published() {
        return List.copyOf(published);
    }

    public void clear() {
        published.clear();
    }
}
