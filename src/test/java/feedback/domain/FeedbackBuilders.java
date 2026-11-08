package feedback.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

/** Test-data builders for ratings, read as domain sentences. */
final class FeedbackBuilders {

    static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    static final LocalDate SATURDAY = LocalDate.of(2026, 11, 14);
    static final FeedbackLimits LAUNCH_LIMITS = new FeedbackLimits(Duration.ofDays(14), 3);

    private FeedbackBuilders() {
    }

    static Instant saturday(String time) {
        return SATURDAY.atTime(LocalTime.parse(time)).atZone(MALAYSIA).toInstant();
    }

    static Instant sunday(String time) {
        return SATURDAY.plusDays(1).atTime(LocalTime.parse(time)).atZone(MALAYSIA).toInstant();
    }

    static Instant daysAfterSaturday(int days, String time) {
        return SATURDAY.plusDays(days).atTime(LocalTime.parse(time)).atZone(MALAYSIA).toInstant();
    }

    /** Became Completed on Saturday 14:00; customer C1, provider P1, named helpers Aina and Ben. */
    static RateableReservation aCompletedReservation() {
        return RateableReservation.completed("R1", "C1", "P1", Set.of("aina", "ben"), saturday("14:00"));
    }

    /** Overall stars, and the same stars for quality of work, with no written feedback. */
    static ProviderRating stars(int overall) {
        return ProviderRating.of(Stars.of(overall), Map.of(ProviderCategory.QUALITY_OF_WORK, Stars.of(overall)), "");
    }

    /** C1 rates the reservation with the given provider stars and no helpers, on Sunday 10:00. */
    static Rating aRecordedRating() {
        return Rating.submit(aCompletedReservation(), "C1", stars(4), Map.of(), LAUNCH_LIMITS, sunday("10:00"));
    }
}
