package com.homa.verification.usecase;

import com.homa.ie.events.PersonVerifiedIE;
import com.homa.verification.domain.CountryPolicy;
import com.homa.verification.domain.IdentityCheck;
import com.homa.verification.domain.IdentityDocument;
import com.homa.verification.domain.IdentityNumber;
import com.homa.verification.domain.PersonName;
import com.homa.verification.domain.VerificationAttempt;
import com.homa.verification.domain.VerificationOutcome;
import com.homa.verification.domain.VerifiedPerson;
import com.homa.verification.domain.exception.VerificationRuleViolationException;
import com.homa.verification.usecase.dto.RecordVerificationResultRequest;
import com.homa.verification.usecase.dto.RecordVerificationResultResponse;
import com.homa.verification.usecase.exception.VerificationNotFoundException;
import com.homa.verification.usecase.port.in.VerificationResultRecorder;
import com.homa.verification.usecase.port.out.EventIds;
import com.homa.verification.usecase.port.out.ProcessedMessageStore;
import com.homa.verification.usecase.port.out.TimeSource;
import com.homa.verification.usecase.port.out.VerificationAnswer;
import com.homa.verification.usecase.port.out.VerificationAttemptRepository;
import com.homa.verification.usecase.port.out.VerificationClient;
import com.homa.verification.usecase.port.out.VerificationIEPublisher;
import com.homa.verification.usecase.port.out.VerifiedPersonRepository;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The verification service's webhook: a check has an answer. Homa reads the answer through the client, maps it
 * into an {@link IdentityCheck} with the country policy's document, looks the identity number up (IDN-3), and lets
 * {@link VerifiedPerson#verify} decide. A pass creates the verified person, or verifies them again under the same
 * personId (a member who becomes a customer), and publishes {@code PersonVerifiedIE}. A refusal (the document not
 * passed or not the one for their status, under 18, a status the role doesn't allow, a number another person holds)
 * is recorded on the verification with its rule and returned; it publishes nothing.
 *
 * <p>A repeated notification is a no-op (the service's notification id, as for gateway webhooks), and a decided
 * verification is never decided again: a later notification for it just returns the recorded outcome.
 *
 * <p>The verification and the verified person are saved in one transaction. The verification carries no events, so
 * the per-aggregate event order is untouched.
 */
public class RecordVerificationResult implements VerificationResultRecorder {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "verification.VerificationServiceWebhook";

    private final CountryPolicy policy;
    private final ProcessedMessageStore processedMessages;
    private final VerificationAttemptRepository attempts;
    private final VerifiedPersonRepository persons;
    private final VerificationClient verificationService;
    private final VerificationIEPublisher publisher;
    private final EventIds eventIds;
    private final TimeSource time;

    public RecordVerificationResult(CountryPolicy policy, ProcessedMessageStore processedMessages,
            VerificationAttemptRepository attempts, VerifiedPersonRepository persons,
            VerificationClient verificationService, VerificationIEPublisher publisher, EventIds eventIds,
            TimeSource time) {
        this.policy = policy;
        this.processedMessages = processedMessages;
        this.attempts = attempts;
        this.persons = persons;
        this.verificationService = verificationService;
        this.publisher = publisher;
        this.eventIds = eventIds;
        this.time = time;
    }

    @Override
    public RecordVerificationResultResponse record(RecordVerificationResultRequest request) {
        if (!processedMessages.recordFirstHandling(request.notificationId(), HANDLER)) {
            return new RecordVerificationResultResponse(RecordVerificationResultResponse.DUPLICATE, null, null, null,
                    null, null);
        }
        VerificationAttempt attempt = attempts.findByServiceReference(request.serviceReference())
                .orElseThrow(() -> new VerificationNotFoundException(request.serviceReference()));
        if (attempt.isPending()) {
            verificationService.resultOf(request.serviceReference()).ifPresent(answer -> decide(attempt, answer));
        }
        return responseFor(attempt);
    }

    private void decide(VerificationAttempt attempt, VerificationAnswer answer) {
        Instant now = time.now();
        try {
            switch (answer) {
                case VerificationAnswer.Passed passed -> verify(attempt, passed, now);
                case VerificationAnswer.NotPassed notPassed -> attempt.recordUnpassedCheck(now);
            }
        } catch (VerificationRuleViolationException refusal) {
            attempt.recordRefusal(refusal, now);
        }
        attempts.save(attempt);
    }

    private void verify(VerificationAttempt attempt, VerificationAnswer.Passed passed, Instant now) {
        IdentityCheck check = checkFrom(passed);
        VerifiedPerson person = VerifiedPerson.verify(attempt.personId(), attempt.role(), check, policy,
                persons.identityNumberStatusOf(check.document().number()));
        attempt.recordPass(now);
        persons.save(person);
        publisher.publish(personVerified(person, attempt, now));
    }

    private IdentityCheck checkFrom(VerificationAnswer.Passed passed) {
        IdentityDocument document = new IdentityDocument(policy.requireDocumentNamed(passed.documentType()),
                new IdentityNumber(passed.documentNumber()));
        return new IdentityCheck(new PersonName(passed.name()), passed.status(), document, passed.dateOfBirth(),
                passed.documentExpiry(), VerificationOutcome.PASSED, passed.checkedAt());
    }

    /** Field by field from the saved person; the identity number is never on it (IDN-3). */
    private PersonVerifiedIE personVerified(VerifiedPerson person, VerificationAttempt attempt, Instant now) {
        return new PersonVerifiedIE(
                eventIds.next(),
                person.id().value(),
                attempt.role().name(),
                attempt.accountId().orElse(null),
                person.name().value(),
                person.status().name(),
                person.result().ageCheckPassed(),
                person.permitExpiry().map(LocalDate::toString).orElse(null),
                person.nextDueAt(attempt.role()).orElse(null),
                now);
    }

    private static RecordVerificationResultResponse responseFor(VerificationAttempt attempt) {
        return new RecordVerificationResultResponse(
                attempt.state().name(),
                attempt.id().value(),
                attempt.personId().value(),
                attempt.role().name(),
                attempt.refusedUnder().orElse(null),
                attempt.refusalReason().orElse(null));
    }
}
