package catalogue.support;

import catalogue.domain.Job;
import catalogue.usecase.port.out.JobRepository;
import support.InMemoryStore;

public final class InMemoryJobRepository extends InMemoryStore<Job> implements JobRepository {

    public InMemoryJobRepository() {
        super(Job::id);
    }
}
