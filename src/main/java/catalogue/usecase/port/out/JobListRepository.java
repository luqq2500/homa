package catalogue.usecase.port.out;

import catalogue.domain.JobList;
import java.util.Optional;

/** Keeps Homa's one job list. */
public interface JobListRepository {

    /** The job list, or empty before the first job is added. */
    Optional<JobList> find();

    void save(JobList jobList);
}
