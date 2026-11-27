package com.homa.verification.domain;

import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class AgeRequirementTest {

    private static final AgeRequirement ADULT = AgeRequirement.ADULT;

    @Test
    @Tag("CUS-10")
    @Tag("PRV-1")
    @Tag("HLP-1")
    @DisplayName("Every customer, owner and member must be aged 18 or above.")
    void everyCustomerOwnerAndMemberMustBeAtLeast18() {
        assertThat(ADULT.minimumAge()).isEqualTo(18);
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Aisyah's 16-year-old son is not of age when he taps Reserve.")
    void aSixteenYearOldIsNotOfAge() {
        assertThat(ADULT.isMetBy(LocalDate.of(2010, 6, 14), at(2026, 11, 30, 19, 55))).isFalse();
        assertThat(ADULT.isMetBy(LocalDate.of(1990, 3, 12), at(2026, 11, 30, 19, 55))).isTrue();
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("A helper verified on their 18th birthday is of age; one whose birthday is the next day is not.")
    void aPersonIsOfAgeFromTheir18thBirthday() {
        Instant checkedAt = at(2026, 11, 2, 14, 0);

        assertThat(ADULT.isMetBy(LocalDate.of(2008, 11, 2), checkedAt)).isTrue();
        assertThat(ADULT.isMetBy(LocalDate.of(2008, 11, 3), checkedAt)).isFalse();
        assertThat(ADULT.isMetBy(LocalDate.of(2008, 11, 1), checkedAt)).isTrue();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("At 00:30 in Kuala Lumpur on her 18th birthday an owner is of age, though it is still the day before in UTC.")
    void ageCountsOnTheKualaLumpurDate() {
        LocalDate born = LocalDate.of(2008, 12, 1);

        assertThat(ADULT.isMetBy(born, at(2026, 12, 1, 0, 30))).isTrue();
        assertThat(ADULT.isMetBy(born, at(2026, 11, 30, 23, 59))).isFalse();
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("A person born on 29 February comes of age on 1 March in a year without 29 February.")
    void aPersonBornOn29FebruaryComesOfAgeOn1March() {
        LocalDate born = LocalDate.of(2008, 2, 29);

        assertThat(ADULT.isMetBy(born, at(2026, 2, 28, 12, 0))).isFalse();
        assertThat(ADULT.isMetBy(born, at(2026, 3, 1, 12, 0))).isTrue();
    }
}
