package com.homa.reservation.usecase.dto;

import java.util.List;

/** The provider marks the job Completed with its after-photos (JOB-3).
 *
 * @param photoRefs the photos already uploaded to the photo store */
public record CompleteReservationRequest(String reservationId, String providerId, List<String> photoRefs) {
    public CompleteReservationRequest {
        photoRefs = List.copyOf(photoRefs);
    }
}
