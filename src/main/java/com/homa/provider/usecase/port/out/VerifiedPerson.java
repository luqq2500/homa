package com.homa.provider.usecase.port.out;

import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.VerifiedPersonFacts;
import java.util.Objects;

/**
 * Provider's copy of what Verification last decided about a person who verified as an owner or a member
 * ({@code PersonVerifiedIE}). Opening a provider (PRV-1) and adding a member (HLP-1 to HLP-3) are API calls, so the
 * facts must already be here when the owner acts.
 *
 * @param facts what Verification decided: local or foreign, the age check, the work permit's expiry
 * @param name  the person's verified name (IDN-2), published as a member's {@code displayName} (ROS-6)
 */
public record VerifiedPerson(VerifiedPersonFacts facts, String name) {

    public VerifiedPerson {
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(name, "name");
    }

    public PersonId personId() {
        return facts.personId();
    }
}
