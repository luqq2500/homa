package com.homa.provider.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProviderValuesTest {

    @Test
    @DisplayName("The age and permit requirements read their numbers from Provider's Homa-set values.")
    void requirementsUseProviderValues() {
        assertThat(AgeRequirement.TEAM_MEMBER.minimumAge()).isEqualTo(ProviderValues.MINIMUM_HELPER_AGE_YEARS);
        assertThat(PermitRequirement.FOREIGN_TEAM_MEMBER.minimumDaysLeft())
                .isEqualTo(ProviderValues.WORK_PERMIT_BUFFER_DAYS);
        assertThat(Provider.MINIMUM_ACTIVE_HELPERS).isEqualTo(ProviderValues.MINIMUM_ACTIVE_HELPERS);
        assertThat(new Eligibility.ForeignWorker(LocalDate.of(2027, 1, 1), true).rosterableUntil())
                .contains(LocalDate.of(2027, 1, 1).minusDays(ProviderValues.WORK_PERMIT_BUFFER_DAYS));
    }
}
