package catalogue.usecase.port.out;

import catalogue.domain.JobList;
import catalogue.usecase.exception.ChangedMeanwhileException;
import java.util.Optional;

/**
 * Keeps Homa's one job list.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found, for example because another founder saved first. The founder then tries again on what is
 * there now; nothing is overwritten.
 */
public interface JobListRepository {

    /** The job list, or empty before the first job is added. */
    Optional<JobList> find();

    void save(JobList jobList);
}
