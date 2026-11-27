package com.homa.provider.usecase.port.out;

import com.homa.provider.domain.PersonId;
import java.util.Optional;

/**
 * Verification's latest word on each owner's and member's face photo (IDN-4), kept per personId from
 * {@code ProfilePhotoChangedIE}. A person's photo can be verified before Provider adds them to a team, so the fact is
 * kept here and applied when they are added.
 */
public interface ProfilePhotoReplica {

    /** True when the person's photo is verified, false when it isn't; empty while Verification hasn't said. */
    Optional<Boolean> photoVerifiedOf(PersonId personId);

    /** Keeps Verification's latest word for the person, replacing the earlier one. */
    void record(PersonId personId, boolean photoVerified);
}
