package com.homa.verification.domain;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.verification.domain.VerificationFixtures.aisyah;
import static com.homa.verification.domain.VerificationFixtures.azman;
import static com.homa.verification.domain.VerificationFixtures.siti;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class VerificationCycleTest {

    private static final VerificationCycle CYCLE = VerificationCycle.EVERY_SIX_MONTHS;

    @Test
    @Tag("MON-1")
    @DisplayName("Siti, last verified on 1 June, is due on 1 December.")
    void reverificationFallsDueSixMonthsAfterTheLastPass() {
        assertThat(CYCLE.cycleMonths()).isEqualTo(6);
        assertThat(CYCLE.nextDueAt(at(2026, 6, 1, 10, 0))).isEqualTo(at(2026, 12, 1, 10, 0));
    }

    @Test
    @Tag("MON-1")
    @DisplayName("Siti re-verifies on 20 November and is next due on 20 May.")
    void aMemberIsNextDueSixMonthsAfterTheirLatestPass() {
        assertThat(siti().nextDueAt(PersonRole.MEMBER)).contains(at(2027, 5, 20, 10, 0));
    }

    @Test
    @Tag("MON-1")
    @DisplayName("A pass on 31 August falls due on the last day of February.")
    void aPassOnTheLastDayOfAMonthFallsDueOnTheLastDayOfTheDueMonth() {
        assertThat(CYCLE.nextDueAt(at(2026, 8, 31, 9, 0))).isEqualTo(at(2027, 2, 28, 9, 0));
    }

    @Test
    @Tag("MON-1")
    @DisplayName("Azman re-verifies as an owner every 6 months; Aisyah, a customer, doesn't re-verify.")
    void ownersReverifyAndCustomersDoNot() {
        assertThat(azman().nextDueAt(PersonRole.OWNER)).contains(at(2027, 4, 1, 9, 0));
        assertThat(aisyah().nextDueAt(PersonRole.CUSTOMER)).isEmpty();
        assertThat(PersonRole.CUSTOMER.reverifies()).isFalse();
    }
}
