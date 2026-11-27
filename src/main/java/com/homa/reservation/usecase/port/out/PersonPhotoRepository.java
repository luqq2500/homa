package com.homa.reservation.usecase.port.out;

import com.homa.reservation.domain.replica.PersonPhoto;
import java.util.Optional;

/**
 * Each person's verified profile photo, kept by person because Verification may verify it before Provider activates
 * the person as a team member (IDN-4, ROS-6).
 */
public interface PersonPhotoRepository {

    Optional<PersonPhoto> findByPersonId(String personId);

    void save(PersonPhoto photo);
}
