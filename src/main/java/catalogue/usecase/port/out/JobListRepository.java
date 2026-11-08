package catalogue.usecase.port.out;

import catalogue.domain.JobList;
import java.util.Optional;

/** Where Homa's job list is kept. There is one job list. */
public interface JobListRepository {

    /** The job list, or empty before Homa adds the first job. */
    Optional<JobList> load();

    void save(JobList jobList);
}
