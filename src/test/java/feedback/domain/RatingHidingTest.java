package feedback.domain;

import static feedback.domain.FeedbackBuilders.aRecordedRating;
import static feedback.domain.FeedbackBuilders.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feedback.domain.exception.HidingReasonRequiredException;
import feedback.domain.exception.RatingAlreadyHiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Feature: Hiding a rating. */
class RatingHidingTest {

    @Test
    void homaHidesARatingWithAReason() {
        Rating rating = aRecordedRating();

        rating.hide(" Personal phone number in the text ", sunday("12:00"));

        assertThat(rating.isHidden()).isTrue();
        assertThat(rating.hiddenReason()).contains("Personal phone number in the text");
        assertThat(rating.hiddenAt()).contains(sunday("12:00"));
    }

    @Test
    void aShownRatingHasNoHidingRecord() {
        Rating rating = aRecordedRating();

        assertThat(rating.hiddenReason()).isEmpty();
        assertThat(rating.hiddenAt()).isEmpty();
    }

    @ParameterizedTest(name = "hiding with reason \"{0}\" is rejected")
    @NullSource
    @ValueSource(strings = {"", "  "})
    void hidingNeedsAReason(String reason) {
        Rating rating = aRecordedRating();

        assertThatThrownBy(() -> rating.hide(reason, sunday("12:00"))).isInstanceOf(HidingReasonRequiredException.class);
        assertThat(rating.isHidden()).isFalse();
    }

    @Test
    void aRatingIsHiddenOnce() {
        Rating rating = aRecordedRating();
        rating.hide("Abuse", sunday("12:00"));

        assertThatThrownBy(() -> rating.hide("Abuse again", sunday("13:00")))
                .isInstanceOf(RatingAlreadyHiddenException.class)
                .hasMessageContaining(rating.id());
        assertThat(rating.hiddenReason()).contains("Abuse");
    }

    @Test
    void hidingNeedsATime() {
        assertThatThrownBy(() -> aRecordedRating().hide("Abuse", null)).isInstanceOf(NullPointerException.class);
    }
}
