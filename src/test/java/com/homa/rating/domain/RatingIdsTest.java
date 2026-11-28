package com.homa.rating.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RatingIdsTest {

    @Test
    @Tag("RTG-1")
    @DisplayName("Every id Rating holds is a non-blank text.")
    void everyIdNeedsText() {
        assertThatThrownBy(() -> new ReservationId(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ReservationId");
        assertThatThrownBy(() -> new CustomerId(" ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CustomerId");
        assertThatThrownBy(() -> new ProviderId("")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ProviderId");
        assertThatThrownBy(() -> new RatingId(" ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RatingId");
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("A new rating id is unique, so Support can name one rating.")
    void generatedRatingIdsAreUnique() {
        assertThat(RatingId.generate()).isNotEqualTo(RatingId.generate());
        assertThat(RatingId.generate().value()).isNotBlank();
    }
}
