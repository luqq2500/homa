package kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimeRangeTest {

    private static Instant at(int hour) {
        return Instant.parse("2026-03-07T00:00:00Z").plus(Duration.ofHours(hour));
    }

    private static TimeRange range(int from, int to) {
        return TimeRange.of(at(from), at(to));
    }

    @Test
    void aRangeStartsBeforeItEnds() {
        assertThatThrownBy(() -> TimeRange.of(at(10), at(10))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TimeRange.of(at(11), at(10))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRangeCanBeGivenByItsStartAndLength() {
        assertThat(TimeRange.startingAt(at(10), Duration.ofHours(3))).isEqualTo(range(10, 13));
        assertThat(range(10, 13).length()).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void rangesThatShareTimeOverlap() {
        assertThat(range(10, 12).overlaps(range(11, 13))).isTrue();
        assertThat(range(11, 13).overlaps(range(10, 12))).isTrue();
        assertThat(range(10, 14).overlaps(range(11, 12))).isTrue();
    }

    @Test
    void aRangeThatEndsExactlyWhenAnotherStartsDoesNotOverlapIt() {
        assertThat(range(10, 12).overlaps(range(12, 14))).isFalse();
        assertThat(range(12, 14).overlaps(range(10, 12))).isFalse();
        assertThat(range(10, 11).overlaps(range(12, 14))).isFalse();
    }

    @Test
    void aRangeCoversAnotherThatLiesWhollyInsideIt() {
        assertThat(range(10, 14).covers(range(10, 14))).isTrue();
        assertThat(range(10, 14).covers(range(11, 13))).isTrue();
        assertThat(range(10, 14).covers(range(9, 13))).isFalse();
        assertThat(range(10, 14).covers(range(11, 15))).isFalse();
    }

    @Test
    void aRangeIsWidenedOnBothSides() {
        assertThat(range(10, 12).widenedBy(Duration.ofHours(1))).isEqualTo(range(9, 13));
    }

    @Test
    void takingOutARangeThatDoesNotOverlapLeavesTheRangeWhole() {
        assertThat(range(10, 12).minus(range(12, 13))).containsExactly(range(10, 12));
    }

    @Test
    void takingOutTheMiddleLeavesTwoPieces() {
        assertThat(range(10, 16).minus(range(12, 13))).containsExactly(range(10, 12), range(13, 16));
    }

    @Test
    void takingOutAnEndLeavesOnePiece() {
        assertThat(range(10, 16).minus(range(9, 12))).containsExactly(range(12, 16));
        assertThat(range(10, 16).minus(range(10, 12))).containsExactly(range(12, 16));
        assertThat(range(10, 16).minus(range(14, 17))).containsExactly(range(10, 14));
        assertThat(range(10, 16).minus(range(14, 16))).containsExactly(range(10, 14));
    }

    @Test
    void takingOutEverythingLeavesNothing() {
        assertThat(range(10, 12).minus(range(9, 13))).isEmpty();
        assertThat(range(10, 12).minus(range(10, 12))).isEmpty();
    }

    @Test
    void backToBackRangesCountAsOneStretch() {
        assertThat(TimeRange.merge(List.of(range(12, 14), range(10, 12)))).containsExactly(range(10, 14));
    }

    @Test
    void overlappingRangesAreJoinedAndSeparateOnesKeptInOrder() {
        assertThat(TimeRange.merge(List.of(range(15, 16), range(10, 13), range(11, 12), range(12, 14))))
                .containsExactly(range(10, 14), range(15, 16));
        assertThat(TimeRange.merge(List.of())).isEmpty();
    }

    @Test
    void blockedRangesAreTakenOutOfTheMergedRanges() {
        List<TimeRange> open = TimeRange.subtract(
                List.of(range(8, 10), range(10, 12), range(14, 18)),
                List.of(range(9, 11), range(15, 16)));
        assertThat(open).containsExactly(range(8, 9), range(11, 12), range(14, 15), range(16, 18));
    }

    @Test
    void aBlockThatEndsExactlyWhenARangeStartsDoesNotBlockIt() {
        assertThat(TimeRange.subtract(List.of(range(10, 12)), List.of(range(8, 10)))).containsExactly(range(10, 12));
        assertThat(TimeRange.subtract(List.of(range(10, 12)), List.of())).containsExactly(range(10, 12));
    }
}
