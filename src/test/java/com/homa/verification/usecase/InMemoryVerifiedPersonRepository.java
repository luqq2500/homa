package com.homa.verification.usecase;

import com.homa.verification.domain.IdentityNumber;
import com.homa.verification.domain.IdentityNumberStatus;
import com.homa.verification.domain.PersonId;
import com.homa.verification.domain.VerifiedPerson;
import com.homa.verification.usecase.port.out.VerifiedPersonRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Verified people in memory, keyed by personId. */
public final class InMemoryVerifiedPersonRepository implements VerifiedPersonRepository {

    private final Map<PersonId, VerifiedPerson> byId = new LinkedHashMap<>();

    @Override
    public Optional<VerifiedPerson> findById(PersonId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public IdentityNumberStatus identityNumberStatusOf(IdentityNumber number) {
        return byId.values().stream()
                .filter(person -> person.identityNumber().equals(number))
                .findFirst()
                .map(person -> IdentityNumberStatus.heldBy(person.id()))
                .orElse(IdentityNumberStatus.free());
    }

    @Override
    public void save(VerifiedPerson person) {
        byId.put(person.id(), person);
    }

    public int size() {
        return byId.size();
    }
}
