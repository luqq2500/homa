package catalogue.usecase;

import catalogue.domain.JobList;
import catalogue.domain.ServiceArea;
import catalogue.domain.Showcase;
import catalogue.domain.ToolList;
import catalogue.usecase.port.out.AdminAction;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.exception.ChangedMeanwhileException;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.JobListRepository;
import catalogue.usecase.port.out.ServiceAreaRepository;
import catalogue.usecase.port.out.ShowcaseRepository;
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

    /** Refuses the next save, as a repository does when another founder saved first. */
    abstract static class Refusable {
        private boolean refuseNext;

        void anotherFounderSavesFirst() {
            refuseNext = true;
        }

        void refuseIfChangedMeanwhile(String what) {
            if (refuseNext) {
                refuseNext = false;
                throw new ChangedMeanwhileException(what);
            }
        }
    }

    static final class InMemoryJobLists extends Refusable implements JobListRepository {
        private JobList saved;
        int saves;

        @Override
        public Optional<JobList> find() {
            return Optional.ofNullable(saved);
        }

        @Override
        public void save(JobList jobList) {
            refuseIfChangedMeanwhile("The job list");
            saved = jobList;
            saves++;
        }
    }

    static final class InMemoryShowcases extends Refusable implements ShowcaseRepository {
        private Showcase saved;
        int saves;

        @Override
        public Optional<Showcase> find() {
            return Optional.ofNullable(saved);
        }

        @Override
        public void save(Showcase showcase) {
            refuseIfChangedMeanwhile("The showcase");
            saved = showcase;
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
