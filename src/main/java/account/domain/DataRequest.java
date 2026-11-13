package account.domain;

import account.domain.exception.DataRequestAlreadyHandledException;
import java.time.Instant;
import java.util.Optional;

/** A request to see, correct, copy or delete personal data, and the record of an admin handling it. */
public final class DataRequest {

    private final String id;
    private final String accountId;
    private final DataRequestKind kind;
    private final Instant requestedAt;
    private Handling handling;

    /** Who handled the request, when, and what they noted. */
    public record Handling(String adminId, String note, Instant handledAt) {
    }

    public DataRequest(String id, String accountId, DataRequestKind kind, Instant requestedAt, Handling handling) {
        this.id = id;
        this.accountId = accountId;
        this.kind = kind;
        this.requestedAt = requestedAt;
        this.handling = handling;
    }

    public static DataRequest make(String accountId, DataRequestKind kind, Instant now) {
        return new DataRequest(new DataRequestId().generate(), accountId, kind, now, null);
    }

    public void handle(String adminId, String note, Instant now) {
        if (handling != null) {
            throw new DataRequestAlreadyHandledException(id);
        }
        handling = new Handling(adminId, note, now);
    }

    public String id() {
        return id;
    }

    public String accountId() {
        return accountId;
    }

    public DataRequestKind kind() {
        return kind;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Optional<Handling> handling() {
        return Optional.ofNullable(handling);
    }
}
