package com.homa.verification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Checks the §13 rows Verification owns against {@link VerificationValues}, read from the rules page itself. */
class VerificationValuesTest {

    private static final Path RULES = Path.of("docs/homa-business-rule.md");

    private static Map<String, String> homaSetValues() throws IOException {
        List<String> lines = Files.readAllLines(RULES);
        int start = lines.indexOf("## 13. Homa-set values");
        assertThat(start).as("§13 heading in %s", RULES).isNotNegative();
        Map<String, String> rows = new LinkedHashMap<>();
        lines.subList(start + 1, lines.size()).stream()
                .takeWhile(line -> !line.startsWith("## "))
                .filter(line -> line.startsWith("| ") && !line.startsWith("| Value ") && !line.startsWith("| ---"))
                .map(line -> line.split("\\|"))
                .forEach(cells -> rows.put(cells[1].strip(), cells[2].strip()));
        return rows;
    }

    @Test
    @Tag("CUS-10")
    @Tag("PRV-1")
    @Tag("HLP-1")
    @DisplayName("Minimum age: 18, for customers, owners and members alike.")
    void theMinimumAgeIs18() throws IOException {
        assertThat(String.valueOf(VerificationValues.MINIMUM_AGE_YEARS)).isEqualTo(homaSetValues().get("Minimum age"));
        assertThat(AgeRequirement.ADULT.minimumAge()).isEqualTo(VerificationValues.MINIMUM_AGE_YEARS);
    }

    @Test
    @Tag("MON-1")
    @DisplayName("Re-verification cycle: every 6 months, reminders from 60 days before.")
    void reverificationIsEverySixMonthsWithRemindersFrom60DaysBefore() throws IOException {
        assertThat("Every " + VerificationValues.REVERIFICATION_CYCLE_MONTHS + " months, reminders from "
                + VerificationValues.REVERIFICATION_REMINDER_DAYS + " days before")
                .isEqualTo(homaSetValues().get("Re-verification cycle"));
        assertThat(VerificationCycle.EVERY_SIX_MONTHS.cycleMonths())
                .isEqualTo(VerificationValues.REVERIFICATION_CYCLE_MONTHS);
    }

    @Test
    @Tag("MON-2")
    @DisplayName("Re-verification when flagged: within 14 days.")
    void aFlaggedPersonReverifiesWithin14Days() throws IOException {
        assertThat("Within " + VerificationValues.FLAGGED_REVERIFICATION_DAYS + " days")
                .isEqualTo(homaSetValues().get("Re-verification when flagged"));
    }
}
