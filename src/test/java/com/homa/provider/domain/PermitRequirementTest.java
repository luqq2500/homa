package com.homa.provider.domain;

import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PermitRequirementTest {

    private static final PermitRequirement NINETY_DAYS = PermitRequirement.FOREIGN_TEAM_MEMBER;

    @Test
    @Tag("HLP-3")
    @DisplayName("A foreign member needs at least 90 days left on their work permit when added.")
    void foreignMembersNeed90DaysOnTheirPermit() {
        assertThat(NINETY_DAYS.minimumDaysLeft()).isEqualTo(90);
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("Added on 2 November, a permit expiring on 31 January (90 days) is enough; one expiring on 30 January (89 days) is not.")
    void permitExpiringExactly90DaysAfterBeingAddedIsEnough() {
        Instant addedAt = at(2026, 11, 2, 14, 0);

        assertThat(NINETY_DAYS.isMetBy(LocalDate.of(2027, 1, 31), addedAt)).isTrue();
        assertThat(NINETY_DAYS.isMetBy(LocalDate.of(2027, 1, 30), addedAt)).isFalse();
        assertThat(NINETY_DAYS.isMetBy(LocalDate.of(2027, 5, 21), addedAt)).isTrue();
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("Added at 00:30 in Kuala Lumpur on 1 December (still 30 November in UTC), the 90 days run to 1 March, not 28 February.")
    void permitDaysCountOnTheKualaLumpurCalendar() {
        Instant addedAt = at(2026, 12, 1, 0, 30);

        assertThat(NINETY_DAYS.isMetBy(LocalDate.of(2027, 3, 1), addedAt)).isTrue();
        assertThat(NINETY_DAYS.isMetBy(LocalDate.of(2027, 2, 28), addedAt)).isFalse();
    }

    @Test
    @Tag("ROS-4")
    @DisplayName("Rina's permit expires on 20 March, so she can work jobs up to 20 December.")
    void rosterableUntilIsTheBufferBeforeThePermitExpires() {
        assertThat(NINETY_DAYS.rosterableUntil(LocalDate.of(2027, 3, 20))).isEqualTo(LocalDate.of(2026, 12, 20));
    }
}
