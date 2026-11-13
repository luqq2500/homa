package catalogue.support;

import catalogue.domain.Showcase;
import catalogue.usecase.port.out.ShowcaseRepository;

public final class InMemoryShowcaseRepository implements ShowcaseRepository {

    private Showcase showcase = Showcase.empty();
    private int saves;

    @Override
    public Showcase get() {
        return new Showcase(showcase.searches());
    }

    @Override
    public void save(Showcase showcase) {
        this.showcase = showcase;
        saves++;
    }

    public int saves() {
        return saves;
    }
}
