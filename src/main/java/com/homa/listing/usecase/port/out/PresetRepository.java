package com.homa.listing.usecase.port.out;

import com.homa.listing.domain.Preset;
import java.util.Optional;

/** Homa's one catalogue of presets (OFR-1); empty until an admin first saves it. */
public interface PresetRepository {

    Optional<Preset> find();

    void save(Preset preset);
}
