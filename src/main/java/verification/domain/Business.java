package verification.domain;

import java.time.Instant;
import java.util.Optional;
import verification.domain.exception.BusinessNotVerifiedException;
import verification.domain.exception.BusinessSeriouslyFlaggedException;

/**
 * A business as the KYC provider sees it. Homa records each result and relies on it: whether the
 * business is verified, the date it is verified until, its registered name and its SSM number.
 */
public final class Business {

    private final String id;
    private BusinessStatus status;
    private BusinessStatus statusBeforeFlag;
    private String ssmNumber;
    private String registeredName;
    private Instant verifiedUntil;
    private Instant lastVerifiedAt;

    public Business(String id, BusinessStatus status, BusinessStatus statusBeforeFlag, String ssmNumber,
                    String registeredName, Instant verifiedUntil, Instant lastVerifiedAt) {
        this.id = id;
        this.status = status;
        this.statusBeforeFlag = statusBeforeFlag;
        this.ssmNumber = ssmNumber;
        this.registeredName = registeredName;
        this.verifiedUntil = verifiedUntil;
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public static Business register(String businessId) {
        return new Business(businessId, BusinessStatus.NOT_VERIFIED, null, null, null, null, null);
    }

    /** KYB or a re-verification passed: the verified-until date moves out. */
    public void passed(String ssmNumber, String registeredName, Instant verifiedUntil, Instant now) {
        if (status == BusinessStatus.SERIOUSLY_FLAGGED) {
            throw new BusinessSeriouslyFlaggedException(id);
        }
        this.ssmNumber = ssmNumber;
        this.registeredName = registeredName;
        this.verifiedUntil = verifiedUntil;
        this.lastVerifiedAt = now;
        this.status = BusinessStatus.VERIFIED;
    }

    /**
     * A check failed. A business that was never verified is rejected and told to redo it; a failed
     * re-verification changes nothing. Returns whether the business is now rejected.
     */
    public boolean failed() {
        if (status != BusinessStatus.NOT_VERIFIED && status != BusinessStatus.REJECTED) {
            return false;
        }
        status = BusinessStatus.REJECTED;
        return true;
    }

    /** Re-verification is needed by a date: verified-until moves to it if it is sooner. Returns whether it moved. */
    public boolean reverificationNeededBy(Instant date) {
        if (verifiedUntil == null || !date.isBefore(verifiedUntil)) {
            return false;
        }
        verifiedUntil = date;
        return true;
    }

    /** The business missed its re-verification. Returns whether it lapsed just now. */
    public boolean lapse() {
        if (status != BusinessStatus.VERIFIED) {
            return false;
        }
        status = BusinessStatus.LAPSED;
        return true;
    }

    /** A confirmed serious flag: the business lapses at once. Returns whether it was flagged just now. */
    public boolean flagSeriously() {
        if (status == BusinessStatus.SERIOUSLY_FLAGGED) {
            return false;
        }
        statusBeforeFlag = status;
        status = BusinessStatus.SERIOUSLY_FLAGGED;
        return true;
    }

    /** The KYC provider cleared the flag: the business is back where it stood. Returns whether a flag was cleared. */
    public boolean clearFlag() {
        if (status != BusinessStatus.SERIOUSLY_FLAGGED) {
            return false;
        }
        status = statusBeforeFlag;
        statusBeforeFlag = null;
        return true;
    }

    /** People are checked only for a business whose KYB has passed and has not lapsed. */
    public void requireVerified() {
        if (status != BusinessStatus.VERIFIED) {
            throw new BusinessNotVerifiedException(id);
        }
    }

    public boolean isVerified() {
        return status == BusinessStatus.VERIFIED;
    }

    public String id() {
        return id;
    }

    public BusinessStatus status() {
        return status;
    }

    public Optional<BusinessStatus> statusBeforeFlag() {
        return Optional.ofNullable(statusBeforeFlag);
    }

    public String ssmNumber() {
        return ssmNumber;
    }

    public String registeredName() {
        return registeredName;
    }

    public Instant verifiedUntil() {
        return verifiedUntil;
    }

    public Instant lastVerifiedAt() {
        return lastVerifiedAt;
    }
}
