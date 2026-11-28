package com.homa.rating.usecase;

import com.homa.rating.domain.Party;
import com.homa.rating.domain.ReliabilityRecord;
import com.homa.rating.usecase.exception.ReliabilityRecordChangedException;
import com.homa.rating.usecase.port.out.ReliabilityRecordRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Test only: reliability records by party, and every save in order. */
public class InMemoryReliabilityRecordRepository implements ReliabilityRecordRepository {

    private final Map<Party, ReliabilityRecord> records = new LinkedHashMap<>();
    private final List<ReliabilityRecord> saves = new ArrayList<>();
    private final Set<Party> conflicting = new HashSet<>();

    /** Puts a record in place without counting it as a save. */
    public void given(ReliabilityRecord record) {
        records.put(record.party(), record);
    }

    /** The next save of this party's record finds that someone else saved it first. */
    public void conflictOnNextSave(Party party) {
        conflicting.add(party);
    }

    @Override
    public Optional<ReliabilityRecord> findByParty(Party party) {
        return Optional.ofNullable(records.get(party));
    }

    @Override
    public void save(ReliabilityRecord record) {
        if (conflicting.remove(record.party())) {
            throw new ReliabilityRecordChangedException(record.party().id());
        }
        records.put(record.party(), record);
        saves.add(record);
    }

    public List<ReliabilityRecord> saves() {
        return List.copyOf(saves);
    }
}
