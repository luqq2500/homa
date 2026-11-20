package com.homa.listing.domain;

import static com.homa.listing.domain.ListingFixtures.BATHROOM_CLEANING;
import static com.homa.listing.domain.ListingFixtures.CYBERJAYA;
import static com.homa.listing.domain.ListingFixtures.HEPA_VACUUM;
import static com.homa.listing.domain.ListingFixtures.KITCHEN_CLEANING;
import static com.homa.listing.domain.ListingFixtures.SALAK_TINGGI;
import static com.homa.listing.domain.ListingFixtures.SHAH_ALAM;
import static com.homa.listing.domain.ListingFixtures.STEAM_MOP;
import static com.homa.listing.domain.ListingFixtures.WINDOW_CLEANING;
import static com.homa.listing.domain.ListingFixtures.homaPresets;
import static com.homa.listing.domain.ListingFixtures.kemasHomeOfferings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-1")
class PresetTest {

    @Test
    @DisplayName("Homa admins add the job types, tools and cities providers may choose from")
    void adminsAddJobTypesToolsAndCitiesToThePresets() {
        Preset preset = Preset.empty();

        preset.addJobType(KITCHEN_CLEANING);
        preset.addTool(HEPA_VACUUM);
        preset.addCity(SALAK_TINGGI);
        preset.addCity(SALAK_TINGGI);

        assertThat(preset.jobTypes()).containsExactly(KITCHEN_CLEANING);
        assertThat(preset.tools()).containsExactly(HEPA_VACUUM);
        assertThat(preset.cities()).containsExactly(SALAK_TINGGI);
    }

    @Test
    @DisplayName("Kemas Home's kitchen, laundry and bathroom cleaning, HEPA vacuum and three cities are all presets")
    void kemasHomesChoicesAreAllPresets() {
        assertThat(homaPresets().notInPresets(kemasHomeOfferings())).isEmpty();
    }

    @Test
    @DisplayName("A job, tool or city Homa doesn't offer is named as not in the presets")
    void choicesOutsideThePresetsAreNamed() {
        Preset preset = Preset.of(Set.of(KITCHEN_CLEANING), Set.of(HEPA_VACUUM), Set.of(SALAK_TINGGI));
        Offerings offerings = new Offerings(Set.of(KITCHEN_CLEANING, WINDOW_CLEANING), Set.of(HEPA_VACUUM, STEAM_MOP),
                Set.of(SALAK_TINGGI, SHAH_ALAM));

        assertThat(preset.notInPresets(offerings)).containsExactly("WINDOW_CLEANING", "STEAM_MOP", "SHAH_ALAM");
    }

    @Test
    @DisplayName("The presets handed out can't be changed by the reader")
    void thePresetsHandedOutAreReadOnly() {
        Preset preset = homaPresets();

        assertThatThrownBy(() -> preset.jobTypes().add(BATHROOM_CLEANING)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> preset.tools().add(STEAM_MOP)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> preset.cities().add(CYBERJAYA)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("A preset code can't be blank")
    void presetCodesCantBeBlank() {
        assertThatThrownBy(() -> new JobType(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Tool("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new City(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JobType(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Tool(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new City(null)).isInstanceOf(NullPointerException.class);
    }
}
