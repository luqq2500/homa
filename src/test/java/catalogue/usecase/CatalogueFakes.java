package catalogue.usecase;

import catalogue.domain.JobList;
import catalogue.domain.ServiceArea;
import catalogue.domain.Shelf;
import catalogue.domain.ToolList;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobListRepository;
import catalogue.usecase.port.out.ServiceAreaRepository;
import catalogue.usecase.port.out.ShelfRepository;
import catalogue.usecase.port.out.ToolListRepository;
import ie.port.CatalogueIE;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** In-memory stand-ins for the catalogue's out ports, for usecase tests. */
final class CatalogueFakes {

    static final Instant NOW = Instant.parse("2026-11-09T02:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private CatalogueFakes() {
    }

    static final class InMemoryJobLists implements JobListRepository {
        JobList stored;
        int saves;

        InMemoryJobLists holding(JobList jobList) {
            stored = jobList;
            return this;
        }

        @Override
        public Optional<JobList> load() {
            return Optional.ofNullable(stored);
        }

        @Override
        public void save(JobList jobList) {
            stored = jobList;
            saves++;
        }
    }

    static final class InMemoryToolLists implements ToolListRepository {
        ToolList stored;
        int saves;

        InMemoryToolLists holding(ToolList toolList) {
            stored = toolList;
            return this;
        }

        @Override
        public Optional<ToolList> load() {
            return Optional.ofNullable(stored);
        }

        @Override
        public void save(ToolList toolList) {
            stored = toolList;
            saves++;
        }
    }

    static final class InMemoryShelves implements ShelfRepository {
        final List<Shelf> stored = new ArrayList<>();

        @Override
        public void save(Shelf shelf) {
            stored.add(shelf);
        }

        @Override
        public List<Shelf> all() {
            return List.copyOf(stored);
        }
    }

    static final class InMemoryServiceAreas implements ServiceAreaRepository {
        ServiceArea stored;
        int saves;

        @Override
        public Optional<ServiceArea> load() {
            return Optional.ofNullable(stored);
        }

        @Override
        public void save(ServiceArea area) {
            stored = area;
            saves++;
        }
    }

    static final class RecordingPublisher implements CatalogueIEPublisher {
        final List<CatalogueIE> published = new ArrayList<>();

        @Override
        public void publish(CatalogueIE event) {
            published.add(event);
        }
    }
}
