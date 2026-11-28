package com.homa.rating.usecase.port.out;

import com.homa.rating.domain.Party;
import com.homa.rating.domain.ReliabilityRecord;
import com.homa.rating.usecase.exception.ReliabilityRecordChangedException;
import java.util.Optional;

/** Stores each party's reliability record, with its dated incidents. */
public interface ReliabilityRecordRepository {

    Optional<ReliabilityRecord> findByParty(Party party);

    /**
     * Inserts a new record or stores the changes to a loaded one. The record was loaded at its {@code version()},
     * or is new at version 0; if someone else saved it since, nothing is saved and
     * {@link ReliabilityRecordChangedException} is thrown.
     */
    void save(ReliabilityRecord record);
}
