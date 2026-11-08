package feedback.domain;

import java.util.Objects;
import java.util.Set;

/** What the customer says about one helper who came: stars and any compliments. Helpers get no written feedback. */
public record HelperRating(Stars stars, Set<Compliment> compliments) {

    public HelperRating {
        Objects.requireNonNull(stars);
        compliments = Set.copyOf(compliments);
    }

    public static HelperRating of(Stars stars, Compliment... compliments) {
        return new HelperRating(stars, Set.of(compliments));
    }
}
