package com.homa.listing.usecase;

import static com.homa.listing.usecase.ListingTestBed.ADMIN;
import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.listing.domain.City;
import com.homa.listing.domain.JobType;
import com.homa.listing.domain.Preset;
import com.homa.listing.domain.Tool;
import com.homa.listing.usecase.dto.AdminManagePresetsRequest;
import com.homa.listing.usecase.dto.AdminManagePresetsResponse;
import com.homa.listing.usecase.exception.NotAllowedException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-1")
class AdminManagePresetsTest {

    private final ListingTestBed bed = new ListingTestBed();

    @Test
    @DisplayName("A Homa admin adds job types, tools and cities; the whole catalogue comes back sorted")
    void anAdminAddsPresets() {
        AdminManagePresetsResponse response = bed.managePresets.manage(new AdminManagePresetsRequest(ADMIN, "ADMIN",
                List.of("LAUNDRY_FOLDING", "KITCHEN_CLEANING"), List.of("HEPA_VACUUM"), List.of("SALAK_TINGGI", "CYBERJAYA")));

        assertThat(response).isEqualTo(new AdminManagePresetsResponse(List.of("KITCHEN_CLEANING", "LAUNDRY_FOLDING"),
                List.of("HEPA_VACUUM"), List.of("CYBERJAYA", "SALAK_TINGGI")));
        Preset stored = bed.presets.stored().orElseThrow();
        assertThat(stored.jobTypes()).containsExactlyInAnyOrder(new JobType("KITCHEN_CLEANING"), new JobType("LAUNDRY_FOLDING"));
        assertThat(stored.tools()).containsExactly(new Tool("HEPA_VACUUM"));
        assertThat(stored.cities()).containsExactlyInAnyOrder(new City("SALAK_TINGGI"), new City("CYBERJAYA"));
    }

    @Test
    @DisplayName("Adding to the catalogue keeps what is already there")
    void addingKeepsTheExistingPresets() {
        bed.withHomaPresets();

        AdminManagePresetsResponse response = bed.managePresets.manage(new AdminManagePresetsRequest(ADMIN, "ADMIN",
                List.of("IRONING"), List.of(), List.of("SHAH_ALAM", "KLANG")));

        assertThat(response.jobTypes()).containsExactly("BATHROOM_CLEANING", "IRONING", "KITCHEN_CLEANING",
                "LAUNDRY_FOLDING", "WINDOW_CLEANING");
        assertThat(response.tools()).containsExactly("HEPA_VACUUM", "STEAM_MOP");
        assertThat(response.cities()).containsExactly("CYBERJAYA", "KLANG", "PUTRAJAYA", "SALAK_TINGGI", "SHAH_ALAM");
        assertThat(bed.presets.stored().orElseThrow().cities()).hasSize(5);
    }

    @Test
    @DisplayName("Only a Homa admin manages presets: Azman, a provider owner, is refused and nothing is stored")
    void onlyAnAdminManagesPresets() {
        AdminManagePresetsRequest byAzman = new AdminManagePresetsRequest(AZMAN, "PROVIDER_OWNER",
                List.of("IRONING"), List.of(), List.of());

        assertThatThrownBy(() -> bed.managePresets.manage(byAzman))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining(AZMAN);
        assertThat(bed.presets.stored()).isEmpty();
    }
}
