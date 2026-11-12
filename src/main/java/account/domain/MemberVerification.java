package account.domain;

import account.domain.exception.EmploymentNotConfirmedException;
import account.domain.exception.MemberNotVerifiedException;
import account.domain.exception.PhotoRequiredException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.VerificationDetailsRequiredException;
import account.domain.exception.VerificationNotWaitingException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * Adding a team member, before the person exists on the team. With the person beside them, the provider
 * chooses the role and takes the profile photo with the phone's camera. The person then verifies with the
 * KYC provider on the spot, which also checks the photo is the same person as their verified selfie, and
 * gives their date of birth: a person under 18 isn't added. Once verified, the provider confirms that this
 * named person is lawfully employed or engaged, and the person joins the team. Until then they are not on
 * the team, never shown to customers, and cannot be named for jobs.
 */
public final class MemberVerification {

    /** Ages are counted on the day in Malaysia. */
    static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    static final int MINIMUM_AGE = 18;

    private final String id;
    private final String providerId;
    private final TeamRole role;
    private final Photo photo;
    private final Instant startedAt;
    private MemberVerificationStatus status = MemberVerificationStatus.WAITING;
    private MemberRejection rejection;
    private EmploymentConfirmation confirmation;
    private String registeredName;
    private Instant verifiedUntil;
    private Instant verifiedAt;

    private MemberVerification(String providerId, TeamRole role, Photo photo, Instant now) {
        this.id = new MemberVerificationId().generate();
        this.providerId = providerId;
        this.role = role;
        this.photo = photo;
        this.startedAt = now;
    }

    /**
     * Starts adding a team member for {@code business}, with the role and the live profile photo. Only a
     * business whose KYB has passed, and that has not lapsed, adds team members, so nobody's identity is
     * checked for a business that may never be verified.
     */
    public static MemberVerification start(ProviderAccount business, TeamRole role, Photo photo, Instant now) {
        if (business.verificationStatus() != VerificationStatus.VERIFIED) {
            throw new ProviderNotVerifiedException(business.id(), business.verificationStatus());
        }
        Objects.requireNonNull(role);
        if (photo == null) {
            throw new PhotoRequiredException();
        }
        return new MemberVerification(business.id(), role, photo, Objects.requireNonNull(now));
    }

    /**
     * Records the KYC provider's result and its comparison of the profile photo with the verified selfie.
     * Verified, adult and matching, the person waits for the provider's confirmation. Otherwise the
     * verification has ended, nothing is added, and the rejection says why. A repeated result changes
     * nothing.
     */
    public VerificationOutcome recordKycResult(KycResult result, PhotoMatch photoMatch, Instant now) {
        Objects.requireNonNull(photoMatch);
        if (status != MemberVerificationStatus.WAITING) {
            return repeatedResult(result);
        }
        Optional<MemberRejection> refused = rejectionFor(result, photoMatch, now);
        if (refused.isPresent()) {
            status = MemberVerificationStatus.REJECTED;
            rejection = refused.get();
            return VerificationOutcome.REJECTED;
        }
        registeredName = result.registeredName().orElseThrow();
        verifiedUntil = result.verifiedUntil().orElseThrow();
        verifiedAt = now;
        status = MemberVerificationStatus.VERIFIED;
        return VerificationOutcome.VERIFIED;
    }

    private static Optional<MemberRejection> rejectionFor(KycResult result, PhotoMatch photoMatch, Instant now) {
        if (!result.isVerified()) {
            return Optional.of(MemberRejection.CHECK_REJECTED);
        }
        LocalDate born = result.dateOfBirth().orElseThrow(() -> new VerificationDetailsRequiredException("a date of birth"));
        if (born.plusYears(MINIMUM_AGE).isAfter(LocalDate.ofInstant(now, MALAYSIA))) {
            return Optional.of(MemberRejection.UNDER_18);
        }
        return photoMatch == PhotoMatch.MATCHED ? Optional.empty() : Optional.of(MemberRejection.PHOTO_DOES_NOT_MATCH);
    }

    /**
     * The provider confirms that the verified, named person is lawfully employed by or engaged with them
     * and allowed to work in Malaysia. Only a verified person can be confirmed; a {@code null} confirmation
     * is no tick.
     */
    public void confirmEmployment(EmploymentConfirmation given) {
        if (status != MemberVerificationStatus.VERIFIED) {
            throw new MemberNotVerifiedException(id, status);
        }
        if (given == null) {
            throw new EmploymentNotConfirmedException();
        }
        confirmation = given;
    }

    /** Whether it ended without adding anyone, so the provider can start again. */
    public boolean hasEnded() {
        return status == MemberVerificationStatus.REJECTED;
    }

    void markAdded() {
        status = MemberVerificationStatus.ADDED;
    }

    public String id() {
        return id;
    }

    public String providerId() {
        return providerId;
    }

    public TeamRole role() {
        return role;
    }

    public Photo photo() {
        return photo;
    }

    /** The provider's lawful-employment confirmation, given once the person is verified; empty until then. */
    public Optional<EmploymentConfirmation> confirmation() {
        return Optional.ofNullable(confirmation);
    }

    /** Why the verification ended without adding anyone; empty unless rejected. */
    public Optional<MemberRejection> rejection() {
        return Optional.ofNullable(rejection);
    }

    /** When the provider started adding the person. */
    public Instant startedAt() {
        return startedAt;
    }

    public MemberVerificationStatus status() {
        return status;
    }

    /** The name on the person's identity document, from the KYC result; empty until verified. */
    public Optional<String> registeredName() {
        return Optional.ofNullable(registeredName);
    }

    /** Until when the person is verified; empty until verified. */
    public Optional<Instant> verifiedUntil() {
        return Optional.ofNullable(verifiedUntil);
    }

    /** When the person passed; empty until verified. */
    public Optional<Instant> verifiedAt() {
        return Optional.ofNullable(verifiedAt);
    }

    private VerificationOutcome repeatedResult(KycResult result) {
        if (!isSameAsRecorded(result)) {
            throw new VerificationNotWaitingException(id, status);
        }
        return VerificationOutcome.UNCHANGED;
    }

    /**
     * A pass repeats a verification, or an age or photo rejection; a failure repeats a rejected check. Only a
     * rejected verification has a rejection, so the rejection alone tells them apart.
     */
    private boolean isSameAsRecorded(KycResult result) {
        boolean rejectedCheck = rejection == MemberRejection.CHECK_REJECTED;
        return result.isVerified() != rejectedCheck;
    }
}
