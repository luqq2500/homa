package com.homa.provider.domain;

import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class AgeRequirementTest {

    private static final AgeRequirement EIGHTEEN = AgeRequirement.TEAM_MEMBER;

    @Test
    @Tag("HLP-1")
    @DisplayName("Every helper and supervisor must be aged 18 or above.")
    void teamMembersMustBeAtLeast18() {
        assertThat(EIGHTEEN.minimumAge()).isEqualTo(18);
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("A candidate verified on their 18th birthday is of age; one whose birthday is the next day is not.")
    void candidateIsOfAgeFromTheir18thBirthday() {
        Instant verifiedAt = at(2026, 11, 2, 14, 0);

        assertThat(EIGHTEEN.isMetBy(LocalDate.of(2008, 11, 2), verifiedAt)).isTrue();
        assertThat(EIGHTEEN.isMetBy(LocalDate.of(2008, 11, 3), verifiedAt)).isFalse();
        assertThat(EIGHTEEN.isMetBy(LocalDate.of(2008, 11, 1), verifiedAt)).isTrue();
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("At 00:30 in Kuala Lumpur on their 18th birthday a candidate is of age, though it is still the day before in UTC.")
    void ageCountsOnTheKualaLumpurDate() {
        LocalDate born = LocalDate.of(2008, 12, 1);

        assertThat(EIGHTEEN.isMetBy(born, at(2026, 12, 1, 0, 30))).isTrue();
        assertThat(EIGHTEEN.isMetBy(born, at(2026, 11, 30, 23, 59))).isFalse();
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("A candidate born on 29 February comes of age on 1 March in a year without 29 February.")
    void candidateBornOn29FebruaryComesOfAgeOn1March() {
        LocalDate born = LocalDate.of(2008, 2, 29);

        assertThat(EIGHTEEN.isMetBy(born, at(2026, 2, 28, 12, 0))).isFalse();
        assertThat(EIGHTEEN.isMetBy(born, at(2026, 3, 1, 12, 0))).isTrue();
    }
}
