package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.DANIEL_SUSPENDED_AT;
import static com.homa.customer.domain.CustomerFixtures.SAFETY_AND_MISCONDUCT;
import static com.homa.customer.domain.CustomerFixtures.SEVEN_DAYS_END;
import static com.homa.customer.domain.CustomerFixtures.THIRTY_DAYS_END;
import static com.homa.customer.domain.CustomerFixtures.supportSuspendsDaniel;
import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.exception.InconsistentSuspensionEndException;
import com.homa.customer.domain.exception.MissingSuspensionLengthException;
import com.homa.customer.domain.exception.MissingSuspensionReasonException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SuspensionTest {

    @Test
    @Tag("CUS-4")
    @DisplayName("Support's decision to suspend Daniel says why and for how long.")
    void supportsDecisionGivesAReasonAndALength() {
        assertThatThrownBy(() -> Suspension.decided(SuspensionLength.THIRTY_DAYS, THIRTY_DAYS_END, null,
                DANIEL_SUSPENDED_AT))
                .isInstanceOf(MissingSuspensionReasonException.class);
        assertThatThrownBy(() -> Suspension.decided(SuspensionLength.THIRTY_DAYS, THIRTY_DAYS_END, " ",
                DANIEL_SUSPENDED_AT))
                .isInstanceOf(MissingSuspensionReasonException.class)
                .hasMessageContaining("CUS-4");
        assertThatThrownBy(() -> Suspension.decided(null, THIRTY_DAYS_END, SAFETY_AND_MISCONDUCT,
                DANIEL_SUSPENDED_AT))
                .isInstanceOf(MissingSuspensionLengthException.class)
                .hasMessage("CUS-4: Support's decision must say how long the suspension lasts");
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("A suspension takes effect the moment Support confirms it, not before.")
    void aSuspensionIsInForceFromTheMomentItStarts() {
        for (SuspensionLength length : SuspensionLength.values()) {
            Suspension suspension = supportSuspendsDaniel(length);
            assertThat(suspension.isInForceAt(DANIEL_SUSPENDED_AT.minusNanos(1))).as(length.name()).isFalse();
            assertThat(suspension.isInForceAt(DANIEL_SUSPENDED_AT)).as(length.name()).isTrue();
        }
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Daniel's 7-day suspension ends by itself at the end Support gave, Wednesday 9 December at 15:00.")
    void aSevenDaySuspensionEndsByItselfAtTheEndSupportGave() {
        Suspension sevenDays = supportSuspendsDaniel(SuspensionLength.SEVEN_DAYS);

        assertThat(SuspensionLength.SEVEN_DAYS.endsOnItsOwn()).isTrue();
        assertThat(sevenDays.endsAt()).contains(SEVEN_DAYS_END);
        assertThat(sevenDays.isInForceAt(SEVEN_DAYS_END.minusNanos(1))).isTrue();
        assertThat(sevenDays.isInForceAt(SEVEN_DAYS_END)).isFalse();
        assertThat(sevenDays.isInForceAt(SEVEN_DAYS_END.plusSeconds(1))).isFalse();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Daniel can book again after 30 days: his suspension ends at the end Support gave, 1 January at "
            + "15:00.")
    void aThirtyDaySuspensionEndsByItselfAtTheEndSupportGave() {
        Suspension thirtyDays = supportSuspendsDaniel(SuspensionLength.THIRTY_DAYS);

        assertThat(SuspensionLength.THIRTY_DAYS.endsOnItsOwn()).isTrue();
        assertThat(thirtyDays.endsAt()).contains(THIRTY_DAYS_END);
        assertThat(thirtyDays.isInForceAt(THIRTY_DAYS_END.minusNanos(1))).isTrue();
        assertThat(thirtyDays.isInForceAt(THIRTY_DAYS_END)).isFalse();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("A suspension until lifted never ends by itself.")
    void aSuspensionUntilLiftedHasNoEndOfItsOwn() {
        Suspension untilLifted = supportSuspendsDaniel(SuspensionLength.UNTIL_LIFTED);

        assertThat(SuspensionLength.UNTIL_LIFTED.endsOnItsOwn()).isFalse();
        assertThat(untilLifted.endsAt()).isEmpty();
        assertThat(untilLifted.liftedAt()).isEmpty();
        assertThat(untilLifted.isInForceAt(at(2036, 12, 2, 15, 0))).isTrue();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("A 7- or 30-day suspension Support decides must end after it starts, because it ends by itself.")
    void aSuspensionThatEndsByItselfNeedsAnEndAfterItStarts() {
        assertThatThrownBy(() -> Suspension.decided(SuspensionLength.THIRTY_DAYS, null, SAFETY_AND_MISCONDUCT,
                DANIEL_SUSPENDED_AT))
                .isInstanceOf(InconsistentSuspensionEndException.class)
                .hasMessage("CUS-6: a THIRTY_DAYS suspension ends on its own, so it needs an end after it starts, "
                        + "not none");
        assertThatThrownBy(() -> Suspension.decided(SuspensionLength.SEVEN_DAYS, DANIEL_SUSPENDED_AT,
                SAFETY_AND_MISCONDUCT, DANIEL_SUSPENDED_AT))
                .isInstanceOf(InconsistentSuspensionEndException.class)
                .hasMessage("CUS-6: a SEVEN_DAYS suspension ends on its own, so it needs an end after it starts, "
                        + "not " + DANIEL_SUSPENDED_AT);
        Instant earlier = DANIEL_SUSPENDED_AT.minusSeconds(60);
        assertThatThrownBy(() -> Suspension.decided(SuspensionLength.SEVEN_DAYS, earlier, SAFETY_AND_MISCONDUCT,
                DANIEL_SUSPENDED_AT))
                .isInstanceOf(InconsistentSuspensionEndException.class);

        Instant aMomentLater = DANIEL_SUSPENDED_AT.plusNanos(1);
        assertThat(Suspension.decided(SuspensionLength.SEVEN_DAYS, aMomentLater, SAFETY_AND_MISCONDUCT,
                DANIEL_SUSPENDED_AT).endsAt()).contains(aMomentLater);
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("A suspension until lifted that Support decides has no end of its own.")
    void aSuspensionUntilLiftedCannotHaveAnEnd() {
        assertThatThrownBy(() -> Suspension.decided(SuspensionLength.UNTIL_LIFTED, THIRTY_DAYS_END,
                SAFETY_AND_MISCONDUCT, DANIEL_SUSPENDED_AT))
                .isInstanceOf(InconsistentSuspensionEndException.class)
                .hasMessage("CUS-6: a suspension until lifted has no end of its own, so it can't end at "
                        + THIRTY_DAYS_END);
    }
}
