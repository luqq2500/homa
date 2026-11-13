package catalogue.domain;

/** A group of jobs, such as kitchen or living room, that makes the job list easy to scan. */
public final class JobGroup {

    private final String id;
    private String name;

    public JobGroup(String id, String name) {
        this.id = id;
        this.name = Names.required(name);
    }

    public static JobGroup add(String name) {
        return new JobGroup(new JobGroupId().generate(), name);
    }

    public void rename(String name) {
        this.name = Names.required(name);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }
}
