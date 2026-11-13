package catalogue.support;

import catalogue.domain.JobGroup;
import catalogue.usecase.port.out.JobGroupRepository;
import support.InMemoryStore;

public final class InMemoryJobGroupRepository extends InMemoryStore<JobGroup> implements JobGroupRepository {

    public InMemoryJobGroupRepository() {
        super(JobGroup::id);
    }
}
