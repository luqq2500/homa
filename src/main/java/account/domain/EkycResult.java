package account.domain;

import account.domain.exception.EkycResultNotFinalException;

/** The rule shared by providers and helpers: an eKYC result from Kenal is VERIFIED or REJECTED. */
final class EkycResult {

    private EkycResult() {
    }

    /** Whether {@code result} is VERIFIED; anything but VERIFIED or REJECTED is refused. */
    static boolean isVerified(VerificationStatus result) {
        if (result != VerificationStatus.VERIFIED && result != VerificationStatus.REJECTED) {
            throw new EkycResultNotFinalException();
        }
        return result == VerificationStatus.VERIFIED;
    }
}
