package feedback.domain;

import java.util.Objects;

/** How often one highlight or compliment was picked, as the provider page shows it: "On time · 8". */
public record PickCount(String pick, int count) {

    public PickCount {
        Objects.requireNonNull(pick);
    }
}
