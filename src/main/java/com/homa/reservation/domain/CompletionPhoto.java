package com.homa.reservation.domain;

import java.util.Objects;

/**
 * A reference to an after-photo the provider uploaded when marking the job Completed (JOB-3). It is kept as
 * evidence for reports (JOB-6, NSH-6); the photo itself lives in the photo store.
 */
public record CompletionPhoto(String reference) {

    public CompletionPhoto {
        Objects.requireNonNull(reference, "reference");
    }
}
