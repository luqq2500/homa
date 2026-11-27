package com.homa.provider.usecase;

import com.homa.provider.domain.PersonId;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import com.homa.provider.usecase.port.out.VerifiedPersonReplica;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** The verified-person replica in a map. Holds no rule. */
public final class InMemoryVerifiedPersonReplica implements VerifiedPersonReplica {

    private final Map<PersonId, VerifiedPerson> people = new HashMap<>();

    @Override
    public Optional<VerifiedPerson> findByPersonId(PersonId personId) {
        return Optional.ofNullable(people.get(personId));
    }

    @Override
    public void save(VerifiedPerson person) {
        people.put(person.personId(), person);
    }

    public int count() {
        return people.size();
    }
}
