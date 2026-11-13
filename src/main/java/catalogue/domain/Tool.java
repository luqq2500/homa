package catalogue.domain;

import catalogue.domain.exception.ToolRetiredException;
import java.time.Instant;
import java.util.Optional;

/** A tool on Homa's tool list. Like a job, it is retired and kept, never deleted. */
public final class Tool {

    private final String id;
    private String name;
    private Retirement retirement;

    public Tool(String id, String name, Retirement retirement) {
        this.id = id;
        this.name = Names.required(name);
        this.retirement = retirement;
    }

    public static Tool add(String name) {
        return new Tool(new ToolId().generate(), name, null);
    }

    public void rename(String name) {
        requirePickable();
        this.name = Names.required(name);
    }

    public void retire(String reason, Instant now) {
        requirePickable();
        this.retirement = new Retirement(reason, now);
    }

    private void requirePickable() {
        if (isRetired()) {
            throw new ToolRetiredException(id);
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

    public Optional<Retirement> retirement() {
        return Optional.ofNullable(retirement);
    }
}
