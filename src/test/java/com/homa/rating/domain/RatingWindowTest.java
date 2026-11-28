package com.homa.rating.domain;

import static com.homa.rating.domain.RatingFixtures.COMPLETED_AT;
import static com.homa.rating.domain.RatingFixtures.WINDOW_CLOSES_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RatingWindowTest {

    private final RatingWindow window = RatingWindow.of(COMPLETED_AT, WINDOW_CLOSES_AT);

    @Test
    @Tag("RTG-2")
    @DisplayName("The window is open until Sunday 13 December 14:00 and closed from that moment.")
    void theWindowClosesAtTheClosingMoment() {
        assertThat(window.opensAt()).isEqualTo(COMPLETED_AT);
        assertThat(window.closesAt()).isEqualTo(WINDOW_CLOSES_AT);
        assertThat(window.hasClosedBy(WINDOW_CLOSES_AT.minus(Duration.ofMinutes(1)))).isFalse();
        assertThat(window.hasClosedBy(WINDOW_CLOSES_AT.minusNanos(1))).isFalse();
        assertThat(window.hasClosedBy(WINDOW_CLOSES_AT)).isTrue();
        assertThat(window.hasClosedBy(WINDOW_CLOSES_AT.plus(Duration.ofMinutes(1)))).isTrue();
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("A window opens before it closes, and has both moments.")
    void aWindowOpensBeforeItCloses() {
        assertThatThrownBy(() -> RatingWindow.of(WINDOW_CLOSES_AT, COMPLETED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RatingWindow(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> window.hasClosedBy(null)).isInstanceOf(NullPointerException.class);
    }
}
