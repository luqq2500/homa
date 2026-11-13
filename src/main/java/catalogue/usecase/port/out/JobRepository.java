package catalogue.usecase.port.out;

import catalogue.domain.Job;
import java.util.List;
import java.util.Optional;

public interface JobRepository {

    void save(Job job);

    Optional<Job> find(String id);

    /** All of them, in the order they were added. */
    List<Job> all();
}
