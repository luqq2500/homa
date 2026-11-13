package catalogue.usecase.port.out;

import catalogue.domain.JobGroup;
import java.util.List;
import java.util.Optional;

public interface JobGroupRepository {

    void save(JobGroup jobGroup);

    Optional<JobGroup> find(String id);

    /** All of them, in the order they were added. */
    List<JobGroup> all();
}
