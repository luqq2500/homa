package policy.usecase.port.out;

import java.util.Optional;
import policy.domain.HomaPolicy;

/** Keeps the Homa-set values. Each save is a new version; earlier versions are kept. */
public interface PolicyRepository {

    /** The version in force, or empty before the policy is launched. */
    Optional<HomaPolicy> findCurrent();

    void save(HomaPolicy policy);
}
