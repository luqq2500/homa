package com.homa.provider.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderValuesTest {

    @Test
    @Tag("HLP-3")
    @DisplayName("The work permit buffer is read from Provider's Homa-set values, when adding and for rostering.")
    void permitRequirementUsesTheWorkPermitBuffer() {
        assertThat(ProviderValues.WORK_PERMIT_BUFFER_DAYS).isEqualTo(90);
        assertThat(PermitRequirement.FOREIGN_TEAM_MEMBER.minimumDaysLeft())
                .isEqualTo(ProviderValues.WORK_PERMIT_BUFFER_DAYS);
        assertThat(new Eligibility.ForeignWorker(LocalDate.of(2027, 1, 1), true).rosterableUntil())
                .contains(LocalDate.of(2027, 1, 1).minusDays(ProviderValues.WORK_PERMIT_BUFFER_DAYS));
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A provider needs at least one active helper to be booked.")
    void minimumActiveHelpersIsOne() {
        assertThat(ProviderValues.MINIMUM_ACTIVE_HELPERS).isEqualTo(1);
    }
}
