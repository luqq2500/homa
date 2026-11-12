package account.usecase.port.out;

/** Keeps a record of what Homa's founders did in the admin console: who, what, why and when. */
public interface AdminAuditLog {

    void record(AdminAction action);
}
