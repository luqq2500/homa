package catalogue.domain;

import catalogue.domain.exception.JobRetiredException;
import java.time.Instant;
import java.util.Optional;

/**
 * A job on Homa's job list. Its id is its code: it never changes and is never used for anything
 * else, so a retired job is kept, not deleted.
 */
public final class Job {

    private final String id;
    private String name;
    private String groupId;
    private Retirement retirement;

    public Job(String id, String name, String groupId, Retirement retirement) {
        this.id = id;
        this.name = Names.required(name);
        this.groupId = groupId;
        this.retirement = retirement;
    }

    public static Job add(String name, JobGroup group) {
        return new Job(new JobId().generate(), name, group.id(), null);
    }

    /** Providers keep the job; only what is shown changes. */
    public void rename(String name) {
        requireOfferable();
        this.name = Names.required(name);
    }

    public void moveTo(JobGroup group) {
        requireOfferable();
        this.groupId = group.id();
    }

    public void retire(String reason, Instant now) {
        requireOfferable();
        this.retirement = new Retirement(reason, now);
    }

    /** Only a job that is not retired can be offered, featured or changed. */
    public void requireOfferable() {
        if (isRetired()) {
            throw new JobRetiredException(id);
        }
    }

    public boolean isRetired() {
        return retirement != null;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String groupId() {
        return groupId;
    }

    public Optional<Retirement> retirement() {
        return Optional.ofNullable(retirement);
    }
}
