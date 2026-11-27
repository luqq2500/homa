package com.homa.listing.usecase.port.in;

import com.homa.listing.usecase.dto.AdminManagePresetsRequest;
import com.homa.listing.usecase.dto.AdminManagePresetsResponse;

/** A Homa admin adds presets providers choose from (OFR-1). */
public interface PresetManager {

    AdminManagePresetsResponse manage(AdminManagePresetsRequest request);
}
