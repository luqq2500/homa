package catalogue.support;

import catalogue.usecase.AdminAddJob;
import catalogue.usecase.AdminAddJobGroup;
import catalogue.usecase.AdminAddTool;
import catalogue.usecase.AdminFeatureSearch;
import catalogue.usecase.dto.AddJobGroupRequest;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.FeatureSearchRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

/** The catalogue's ports, faked in memory, with shortcuts to set up the lists a test needs. */
public final class CatalogueWorld {

    public static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");
    public static final String ADMIN = "admin-1";

    public final InMemoryJobGroupRepository groups = new InMemoryJobGroupRepository();
    public final InMemoryJobRepository jobs = new InMemoryJobRepository();
    public final InMemoryToolRepository tools = new InMemoryToolRepository();
    public final InMemoryShowcaseRepository showcases = new InMemoryShowcaseRepository();
    public final RecordingCatalogueIEPublisher publisher = new RecordingCatalogueIEPublisher();
    public final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    public String group(String name) {
        return new AdminAddJobGroup(groups).add(new AddJobGroupRequest(ADMIN, name)).groupId();
    }

    public String job(String name, String groupId) {
        return new AdminAddJob(jobs, groups, publisher, clock).add(new AddJobRequest(ADMIN, name, groupId)).jobId();
    }

    public String tool(String name) {
        return new AdminAddTool(tools, publisher, clock).add(new AddToolRequest(ADMIN, name)).toolId();
    }

    public String featured(String name, String... jobIds) {
        return new AdminFeatureSearch(showcases, jobs)
                .feature(new FeatureSearchRequest(ADMIN, name, Set.of(jobIds), 2, 1)).searchId();
    }
}
