package account.usecase.port.out;

import account.domain.PhoneVerification;
import account.usecase.exception.ChangedMeanwhileException;
import java.util.Optional;

/**
 * Keeps the phone verifications in progress. A verification is removed once it has registered an
 * account, so it registers only one.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found. Nothing is overwritten; the caller tries again on what is there now.
 */
public interface PhoneVerificationRepository {

    Optional<PhoneVerification> find(String verificationId);

    void save(PhoneVerification verification);

    void remove(String verificationId);
}
