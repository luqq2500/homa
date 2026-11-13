package catalogue.support;

import ie.port.CatalogueIE;
import java.util.ArrayList;
import java.util.List;
import catalogue.usecase.port.out.CatalogueIEPublisher;

public final class RecordingCatalogueIEPublisher implements CatalogueIEPublisher {

    private final List<CatalogueIE> published = new ArrayList<>();

    @Override
    public void publish(CatalogueIE event) {
        published.add(event);
    }

    public List<CatalogueIE> published() {
        return List.copyOf(published);
    }

    /** The published events of one type, in order. */
    public <E extends CatalogueIE> List<E> published(Class<E> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
