package catalogue.domain;

import catalogue.domain.exception.DuplicateJobCodeException;
import catalogue.domain.exception.JobAlreadyRetiredException;
import catalogue.domain.exception.JobNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Homa's job list: the jobs providers pick from and search matches on.
 *
 * <p>Codes are unique. Jobs are retired, never deleted, so a code once used is never reused for a
 * different job.
 */
public final class JobList {

    private final String id;
    private final Map<JobCode, Job> jobs = new LinkedHashMap<>();

    private JobList(String id) {
        this.id = id;
    }

    public static JobList create() {
        return new JobList(new JobListId().generate());
    }

    public void add(JobCode code, String name, JobGroup group) {
        if (jobs.containsKey(code)) {
            throw new DuplicateJobCodeException(code.value());
        }
        jobs.put(code, new Job(code, name, group));
    }

    /** Renames a job. Its code stays. Returns whether the name changed: the same name changes nothing. */
    public boolean rename(JobCode code, String name) {
        return existing(code).rename(name);
    }

    /**
     * Moves a job to another display group, such as from Kitchen to Living room. Its code stays. Returns
     * whether the group changed: the same group changes nothing.
     */
    public boolean regroup(JobCode code, JobGroup group) {
        return existing(code).regroup(group);
    }

    public void retire(JobCode code) {
        Job job = existing(code);
        if (!job.isActive()) {
            throw new JobAlreadyRetiredException(code.value());
        }
        job.retire();
    }

    /** Whether providers can offer this job now: it is on the list and not retired. */
    public boolean isOffered(JobCode code) {
        return job(code).map(Job::isActive).orElse(false);
    }

    public boolean contains(JobCode code) {
        return jobs.containsKey(code);
    }

    public Optional<Job> job(JobCode code) {
        return Optional.ofNullable(jobs.get(code));
    }

    /** Every job, retired ones included, in the order they were added. */
    public List<Job> jobs() {
        return List.copyOf(jobs.values());
    }

    /** The codes providers can offer now. */
    public Set<JobCode> offeredCodes() {
        return jobs.values().stream().filter(Job::isActive).map(Job::code).collect(Collectors.toUnmodifiableSet());
    }

    public String id() {
        return id;
    }

    private Job existing(JobCode code) {
        return job(code).orElseThrow(() -> new JobNotFoundException(code.value()));
    }
}
