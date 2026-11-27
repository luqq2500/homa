package com.homa.verification.usecase;

import com.homa.verification.domain.CountryPolicy;
import com.homa.verification.domain.IdentityDocumentType;
import com.homa.verification.domain.PersonId;
import com.homa.verification.domain.PersonRole;
import com.homa.verification.domain.VerificationAttempt;
import com.homa.verification.domain.VerificationAttemptId;
import com.homa.verification.usecase.dto.StartVerificationRequest;
import com.homa.verification.usecase.dto.StartVerificationResponse;
import com.homa.verification.usecase.exception.UnknownRoleException;
import com.homa.verification.usecase.port.in.VerificationStarter;
import com.homa.verification.usecase.port.out.SignInPersonIds;
import com.homa.verification.usecase.port.out.StartedVerification;
import com.homa.verification.usecase.port.out.TimeSource;
import com.homa.verification.usecase.port.out.VerificationAttemptRepository;
import com.homa.verification.usecase.port.out.VerificationClient;
import com.homa.verification.usecase.port.out.VerificationSubmission;
import java.util.Arrays;

/**
 * A customer, an owner or a member starts identity verification. The signed-in user's personId is settled first:
 * the one they were given at their first verification, or a new one now, so one person keeps one personId across
 * roles (IDN-3). A document the country policy doesn't name is refused before anything reaches the verification
 * service (IDN-1), and so is a customer's verification without the account that asked (CUS-3). The service's answer
 * arrives later, through {@link RecordVerificationResult}.
 *
 * <p>Hooks, not built yet: an owner's or member's privacy consent is recorded here when their verification starts
 * (DAT-11, slice 9), and so is their face photo for the verified profile photo (IDN-4, slice 9).
 */
public class PersonStartVerification implements VerificationStarter {

    private final CountryPolicy policy;
    private final SignInPersonIds signIns;
    private final VerificationClient verificationService;
    private final VerificationAttemptRepository attempts;
    private final TimeSource time;

    public PersonStartVerification(CountryPolicy policy, SignInPersonIds signIns, VerificationClient verificationService,
            VerificationAttemptRepository attempts, TimeSource time) {
        this.policy = policy;
        this.signIns = signIns;
        this.verificationService = verificationService;
        this.attempts = attempts;
        this.time = time;
    }

    @Override
    public StartVerificationResponse start(StartVerificationRequest request) {
        PersonRole role = roleNamed(request.role());
        IdentityDocumentType document = policy.requireDocumentNamed(request.documentType());
        PersonId personId = personIdOf(request.signInId());
        VerificationAttempt attempt = VerificationAttempt.start(VerificationAttemptId.generate(), personId, role,
                request.accountId(), time.now());
        StartedVerification started = verificationService.start(
                new VerificationSubmission(attempt.id().value(), document.name(), request.documentNumber()));
        attempt.submittedAs(started.reference());
        attempts.save(attempt);
        return new StartVerificationResponse(attempt.id().value(), personId.value(), started.handoff(),
                attempt.state().name());
    }

    private static PersonRole roleNamed(String role) {
        return Arrays.stream(PersonRole.values())
                .filter(candidate -> candidate.name().equals(role))
                .findFirst()
                .orElseThrow(() -> new UnknownRoleException(role));
    }

    private PersonId personIdOf(String signInId) {
        return signIns.personIdOf(signInId).orElseGet(() -> {
            PersonId first = PersonId.generate();
            signIns.assign(signInId, first);
            return first;
        });
    }
}
