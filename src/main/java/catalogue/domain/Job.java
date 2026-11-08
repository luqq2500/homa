package catalogue.domain;

import java.util.Objects;

/**
 * A job on Homa's job list. A job is identified by its code; its name can change, and it can be retired
 * so providers can no longer offer it. A job is never deleted.
 *
 * <p>Jobs change only through their {@link JobList}.
 */
public final class Job {

    private final JobCode code;
    private final JobGroup group;
    private String name;
    private boolean active;

    Job(JobCode code, String name, JobGroup group) {
        this.code = Objects.requireNonNull(code);
        this.group = Objects.requireNonNull(group);
        this.name = CatalogueText.requireName(name, "A job");
        this.active = true;
    }

    void rename(String newName) {
        name = CatalogueText.requireName(newName, "A job");
    }

    void retire() {
        active = false;
    }

    public JobCode code() {
        return code;
    }

    public String name() {
        return name;
    }

    public JobGroup group() {
        return group;
    }

    public boolean isActive() {
        return active;
    }
}
