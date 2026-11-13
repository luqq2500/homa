package kernel;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A stretch of time from a start up to, but not including, an end. A range that ends exactly when
 * another starts does not overlap it.
 */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("A time range starts before it ends: " + start + " to " + end);
        }
    }

    public static TimeRange of(Instant start, Instant end) {
        return new TimeRange(start, end);
    }

    public static TimeRange startingAt(Instant start, Duration length) {
        return new TimeRange(start, start.plus(length));
    }

    public Duration length() {
        return Duration.between(start, end);
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** Whether the whole of {@code other} lies inside this range. */
    public boolean covers(TimeRange other) {
        return !other.start.isBefore(start) && !other.end.isAfter(end);
    }

    /** This range widened by {@code margin} on both sides. */
    public TimeRange widenedBy(Duration margin) {
        return new TimeRange(start.minus(margin), end.plus(margin));
    }

    /** What is left of this range once {@code other} is taken out: nothing, one piece or two. */
    public List<TimeRange> minus(TimeRange other) {
        if (!overlaps(other)) {
            return List.of(this);
        }
        List<TimeRange> left = new ArrayList<>(2);
        if (start.isBefore(other.start)) {
            left.add(new TimeRange(start, other.start));
        }
        if (other.end.isBefore(end)) {
            left.add(new TimeRange(other.end, end));
        }
        return List.copyOf(left);
    }

    /** The same time as the given ranges, in order, with overlapping and back-to-back ranges joined. */
    public static List<TimeRange> merge(List<TimeRange> ranges) {
        List<TimeRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparing(TimeRange::start));
        List<TimeRange> merged = new ArrayList<>();
        for (TimeRange next : sorted) {
            int last = merged.size() - 1;
            if (last >= 0 && !merged.get(last).end.isBefore(next.start)) {
                merged.set(last, merged.get(last).joinedWith(next));
            } else {
                merged.add(next);
            }
        }
        return List.copyOf(merged);
    }

    /** The given ranges, merged, with every blocked range taken out. */
    public static List<TimeRange> subtract(List<TimeRange> ranges, List<TimeRange> blocked) {
        List<TimeRange> open = merge(ranges);
        for (TimeRange block : blocked) {
            List<TimeRange> next = new ArrayList<>();
            for (TimeRange range : open) {
                next.addAll(range.minus(block));
            }
            open = next;
        }
        return List.copyOf(open);
    }

    private TimeRange joinedWith(TimeRange next) {
        return new TimeRange(start, end.isAfter(next.end) ? end : next.end);
    }
}
