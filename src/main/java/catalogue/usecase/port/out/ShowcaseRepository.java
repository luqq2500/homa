package catalogue.usecase.port.out;

import catalogue.domain.Showcase;

public interface ShowcaseRepository {

    /** The showcase; empty until a search is featured. */
    Showcase get();

    void save(Showcase showcase);
}
