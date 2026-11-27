package com.homa.reservation.usecase.port.out;

/** The photo store the provider uploads after-photos to before marking a job Completed (JOB-3, JOB-6). */
public interface CompletionPhotoStore {

    /** Whether a photo was uploaded under this reference. */
    boolean holds(String photoRef);
}
