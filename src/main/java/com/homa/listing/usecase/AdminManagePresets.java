package com.homa.listing.usecase;

import com.homa.listing.domain.City;
import com.homa.listing.domain.JobType;
import com.homa.listing.domain.Preset;
import com.homa.listing.domain.Tool;
import com.homa.listing.usecase.dto.AdminManagePresetsRequest;
import com.homa.listing.usecase.dto.AdminManagePresetsResponse;
import com.homa.listing.usecase.exception.NotAllowedException;
import com.homa.listing.usecase.port.in.PresetManager;
import com.homa.listing.usecase.port.out.PresetRepository;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * OFR-1: a Homa admin adds job types, tools and cities providers can choose from. Presets are only added in slice
 * 1: removing one that listings use is a rule question the rules don't answer yet. Publishes nothing; the catalog
 * has no event.
 */
public final class AdminManagePresets implements PresetManager {

    /** The only role that may manage presets. */
    static final String ADMIN_ROLE = "ADMIN";

    private final PresetRepository presets;

    public AdminManagePresets(PresetRepository presets) {
        this.presets = presets;
    }

    @Override
    public AdminManagePresetsResponse manage(AdminManagePresetsRequest request) {
        if (!ADMIN_ROLE.equals(request.actingRole())) {
            throw new NotAllowedException(request.actingPersonId() + " isn't a Homa admin, so can't manage presets");
        }
        Preset preset = presets.find().orElseGet(Preset::empty);
        request.addJobTypes().forEach(code -> preset.addJobType(new JobType(code)));
        request.addTools().forEach(code -> preset.addTool(new Tool(code)));
        request.addCities().forEach(code -> preset.addCity(new City(code)));
        presets.save(preset);
        return new AdminManagePresetsResponse(sortedCodes(preset.jobTypes(), JobType::code),
                sortedCodes(preset.tools(), Tool::code), sortedCodes(preset.cities(), City::code));
    }

    private static <T> List<String> sortedCodes(Set<T> values, Function<T, String> code) {
        return values.stream().map(code).sorted().toList();
    }
}
