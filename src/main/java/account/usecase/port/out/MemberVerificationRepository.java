package account.usecase.port.out;

import account.domain.MemberVerification;
import account.usecase.exception.ChangedMeanwhileException;
import java.util.List;
import java.util.Optional;

/** Keeps the verifications of people being added to a team.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found. Nothing is overwritten; the caller tries again on what is there now.
 */
public interface MemberVerificationRepository {

    Optional<MemberVerification> find(String verificationId);

    /**
     * The business's people not yet on the team: waiting for the KYC provider's result, or verified and
     * waiting for the provider's confirmation. Oldest first.
     */
    List<MemberVerification> findWaitingFor(String providerId);

    void save(MemberVerification verification);
}
