package catalogue.usecase;

import catalogue.domain.JobList;
import catalogue.domain.ServiceArea;
import catalogue.domain.Shelf;
import catalogue.domain.ToolList;
import catalogue.usecase.port.out.AdminAction;
import catalogue.usecase.port.out.AdminAuditLog;
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

/** In-memory stand-ins for the catalogue's out ports, and the values the usecase tests share. */
final class CatalogueUsecaseFixtures {

    /** Saturday 14 November 2026, 10:00 in Malaysia. */
    static final Instant NOW = Instant.parse("2026-11-14T02:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final String FOUNDER = "founder-aisyah";

    private CatalogueUsecaseFixtures() {
    }

    /** What an admin action leaves in the audit log when it needs no reason. */
    static AdminAction recorded(String action, String subject) {
        return new AdminAction(FOUNDER, action, subject, "", NOW);
    }

    static final class InMemoryJobLists implements JobListRepository {
        private JobList saved;
        int saves;

        @Override
        public Optional<JobList> find() {
            return Optional.ofNullable(saved);
        }

        @Override
        public void save(JobList jobList) {
            saved = jobList;
            saves++;
        }
    }

    static final class InMemoryToolLists implements ToolListRepository {
        private ToolList saved;
        int saves;

        @Override
        public Optional<ToolList> find() {
            return Optional.ofNullable(saved);
        }

        @Override
        public void save(ToolList toolList) {
            saved = toolList;
            saves++;
        }
    }

    static final class InMemoryShelves implements ShelfRepository {
        private final List<Shelf> saved = new ArrayList<>();
        int saves;

        @Override
        public void save(Shelf shelf) {
            saved.removeIf(kept -> kept.id().equals(shelf.id()));
            saved.add(shelf);
            saves++;
        }

        Shelf find(String title) {
            return saved.stream().filter(shelf -> shelf.title().equals(title)).findFirst().orElseThrow();
        }

        @Override
        public List<Shelf> findAll() {
            return List.copyOf(saved);
        }
    }

    static final class InMemoryServiceAreas implements ServiceAreaRepository {
        private ServiceArea saved;
        int saves;

        @Override
        public Optional<ServiceArea> find() {
            return Optional.ofNullable(saved);
        }

        @Override
        public void save(ServiceArea serviceArea) {
            saved = serviceArea;
            saves++;
        }
    }

    /** Keeps every event published, in order. */
    static final class PublishedEvents implements CatalogueIEPublisher {
        final List<CatalogueIE> all = new ArrayList<>();

        @Override
        public void publish(CatalogueIE event) {
            all.add(event);
        }
    }

    /** Keeps every admin action recorded, in order. */
    static final class AuditLog implements AdminAuditLog {
        final List<AdminAction> all = new ArrayList<>();

        @Override
        public void record(AdminAction action) {
            all.add(action);
        }
    }
}
