package reservation.usecase.port.out;

import reservation.domain.FaceCheckResult;

/**
 * The KYC provider's face match at the door: a live selfie against the person's verification, by the
 * provider's own rules and attempts. Homa keeps only the result and time, never the selfie.
 */
public interface FaceChecker {

    FaceCheckResult check(String memberId, String selfie);
}
