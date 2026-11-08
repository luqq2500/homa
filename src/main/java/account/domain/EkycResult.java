package account.domain;

import account.domain.exception.EkycResultNotFinalException;

/** The rule shared by providers and helpers: an eKYC result from Kenal is VERIFIED or REJECTED. */
final class EkycResult {

    private EkycResult() {
    }

    static VerificationStatus require(VerificationStatus result) {
        if (result == null || result == VerificationStatus.PENDING) {
            throw new EkycResultNotFinalException();
        }
        return result;
    }
}
