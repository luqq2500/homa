package com.homa.verification.usecase;

import com.homa.verification.domain.VerificationAttempt;
import com.homa.verification.domain.VerificationAttemptState;
import com.homa.verification.usecase.port.out.VerificationAttemptRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Verifications in memory, keyed by id. It keeps the state each save wrote, so a test sees what was saved rather
 * than what the use case changed afterwards.
 */
public final class InMemoryVerificationAttemptRepository implements VerificationAttemptRepository {

    private final Map<String, VerificationAttempt> byId = new LinkedHashMap<>();
    private final Map<String, VerificationAttemptState> savedStates = new LinkedHashMap<>();
    private final List<String> saves = new ArrayList<>();

    @Override
    public Optional<VerificationAttempt> findByServiceReference(String serviceReference) {
        return byId.values().stream()
                .filter(attempt -> attempt.serviceReference().filter(serviceReference::equals).isPresent())
                .findFirst();
    }

    @Override
    public void save(VerificationAttempt attempt) {
        byId.put(attempt.id().value(), attempt);
        savedStates.put(attempt.id().value(), attempt.state());
        saves.add(attempt.id().value());
    }

    public Optional<VerificationAttempt> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** The state the last save of {@code id} wrote. */
    public VerificationAttemptState savedStateOf(String id) {
        return savedStates.get(id);
    }

    /** How many saves were made. */
    public int saveCount() {
        return saves.size();
    }

    public int size() {
        return byId.size();
    }
}
