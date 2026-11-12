package policy.usecase.port.out;

import java.util.Optional;
import policy.domain.HomaPolicy;
import policy.usecase.exception.ChangedMeanwhileException;

/**
 * Keeps the Homa-set values. Each save is a new version; earlier versions are kept.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} unless its version is one more than the
 * version in force, or 1 when none is, so two founders changing values at once can never make two versions
 * with the same number, and neither change is lost. The founder whose save is refused tries again on the
 * version in force now.
 */
public interface PolicyRepository {

    /** The version in force, or empty before the policy is launched. */
    Optional<HomaPolicy> findCurrent();

    void save(HomaPolicy policy);
}
