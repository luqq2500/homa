package com.homa.listing.usecase;

import com.homa.listing.domain.Preset;
import com.homa.listing.usecase.port.out.PresetRepository;
import java.util.Optional;

final class InMemoryPresetRepository implements PresetRepository {

    private Preset stored;

    @Override
    public Optional<Preset> find() {
        return Optional.ofNullable(stored).map(InMemoryPresetRepository::copy);
    }

    @Override
    public void save(Preset preset) {
        stored = copy(preset);
    }

    Optional<Preset> stored() {
        return Optional.ofNullable(stored);
    }

    private static Preset copy(Preset preset) {
        return Preset.of(preset.jobTypes(), preset.tools(), preset.cities());
    }
}
