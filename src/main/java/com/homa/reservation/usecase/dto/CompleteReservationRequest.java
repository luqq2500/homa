package com.homa.reservation.usecase.dto;

import java.util.List;

/**
 * The provider marks the job Completed with its after-photos (JOB-3).
 *
 * @param personId  who signed in: the provider's owner, or a member rostered on the reservation (PRV-11)
 * @param photoRefs the photos already uploaded to the photo store
 */
public record CompleteReservationRequest(String reservationId, String personId, List<String> photoRefs) {
    public CompleteReservationRequest {
        photoRefs = List.copyOf(photoRefs);
    }
}
