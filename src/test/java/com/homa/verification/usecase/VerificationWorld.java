package com.homa.verification.usecase;

import static com.homa.fixtures.HomaFixtures.at;

import com.homa.fixtures.HomaFixtures;
import com.homa.verification.domain.CountryPolicy;
import com.homa.verification.domain.PersonStatus;
import com.homa.verification.usecase.dto.RecordVerificationResultRequest;
import com.homa.verification.usecase.dto.RecordVerificationResultResponse;
import com.homa.verification.usecase.dto.StartVerificationRequest;
import com.homa.verification.usecase.dto.StartVerificationResponse;
import com.homa.verification.usecase.ie.HandlePayoutAccountSubmittedIE;
import com.homa.verification.usecase.port.out.VerificationAnswer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Verification's use cases wired to in-memory fakes under Malaysia's policy, with the §0 people as the verification
 * service would confirm them. Identity numbers are fictional and match the domain's VerificationFixtures.
 */
public final class VerificationWorld {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String OWNER = "OWNER";
    public static final String MEMBER = "MEMBER";

    public static final String AISYAH_SIGN_IN = "sign-in-aisyah";
    public static final String AISYAH_MYKAD = "900312-10-5544";
    public static final Instant AISYAH_CHECKED_AT = HomaFixtures.REQUESTED_AT.minus(Duration.ofMinutes(5));

    public static final String AZMAN_SIGN_IN = "sign-in-azman";
    public static final String AZMAN_MYKAD = "790405-10-5123";
    public static final Instant AZMAN_CHECKED_AT = at(2026, 10, 1, 9, 0);

    public static final String SITI_SIGN_IN = "sign-in-siti";
    public static final String SITI_MYKAD = "960701-10-5246";
    public static final Instant SITI_CHECKED_AT = at(2026, 11, 20, 10, 0);

    public static final String RINA_SIGN_IN = "sign-in-rina";
    public static final String RINA_IKAD = "PW2611-0451";
    public static final Instant RINA_CHECKED_AT = at(2026, 11, 2, 10, 0);
    public static final LocalDate RINA_IKAD_EXPIRY = LocalDate.of(2026, 11, 2).plusDays(200);

    public static final String SON_SIGN_IN = "sign-in-aisyahs-son";
    public static final String SON_CUSTOMER_ID = "customer-aisyahs-son";
    public static final String SON_MYKAD = "100614-10-5577";

    public static final String DANIEL_SIGN_IN = "sign-in-daniel";
    public static final String DANIEL_SECOND_SIGN_IN = "sign-in-daniel-second-google-account";
    public static final String DANIEL_CUSTOMER_ID = "customer-daniel";
    public static final String DANIEL_SECOND_CUSTOMER_ID = "customer-daniel-second";
    public static final String DANIEL_MYKAD = "880817-14-6021";

    public static final LocalDate ADULT_BORN = LocalDate.of(1995, 1, 15);
    /** How long after the check the service's webhook reaches Homa. */
    public static final Duration WEBHOOK_DELAY = Duration.ofSeconds(30);

    public final InMemoryVerifiedPersonRepository persons = new InMemoryVerifiedPersonRepository();
    public final InMemoryVerificationAttemptRepository attempts = new InMemoryVerificationAttemptRepository();
    public final InMemorySignInPersonIds signIns = new InMemorySignInPersonIds();
    public final InMemoryProcessedMessageStore processedMessages = new InMemoryProcessedMessageStore();
    public final InMemoryProviderOwners providerOwners = new InMemoryProviderOwners();
    public final FakeVerificationClient verificationService = new FakeVerificationClient();
    public final FakeAccountHolderCheck holderCheck = new FakeAccountHolderCheck();
    public final RecordingVerificationIEPublisher publisher = new RecordingVerificationIEPublisher();
    public final SequentialEventIds eventIds = new SequentialEventIds();
    public final FixedTimeSource time = new FixedTimeSource(AISYAH_CHECKED_AT.minus(Duration.ofMinutes(1)));

    public final PersonStartVerification starter =
            new PersonStartVerification(CountryPolicy.MALAYSIA, signIns, verificationService, attempts, time);
    public final RecordVerificationResult recorder = new RecordVerificationResult(CountryPolicy.MALAYSIA,
            processedMessages, attempts, persons, verificationService, publisher, eventIds, time);
    public final HandlePayoutAccountSubmittedIE payoutAccountSubmitted = new HandlePayoutAccountSubmittedIE(
            processedMessages, providerOwners, persons, holderCheck, publisher, eventIds, time);

    private int notifications;

    public StartVerificationResponse start(String signInId, String role, String accountId, String documentType) {
        return starter.start(new StartVerificationRequest(signInId, role, accountId, documentType, null));
    }

    /** How the verification service knows the started verification. */
    public String referenceOf(StartVerificationResponse started) {
        return verificationService.referenceOf(started.verificationId());
    }

    /** The service answers the started check, and its webhook reaches Homa shortly after. */
    public RecordVerificationResultResponse answer(StartVerificationResponse started, VerificationAnswer answer) {
        verificationService.answers(referenceOf(started), answer);
        time.set(answer.checkedAt().plus(WEBHOOK_DELAY));
        return webhook(nextNotificationId(), started);
    }

    /** The service's webhook for the started check, with notification id {@code notificationId}. */
    public RecordVerificationResultResponse webhook(String notificationId, StartVerificationResponse started) {
        return recorder.record(new RecordVerificationResultRequest(notificationId, referenceOf(started)));
    }

    /** {@code signInId} starts verifying for {@code role} with the answer's document, and the service answers. */
    public RecordVerificationResultResponse verify(String signInId, String role, String accountId,
            VerificationAnswer.Passed answer) {
        return answer(start(signInId, role, accountId, answer.documentType()), answer);
    }

    public String nextNotificationId() {
        notifications++;
        return "notification-" + notifications;
    }

    public static VerificationAnswer.Passed localMyKad(String name, String number, LocalDate born, Instant checkedAt) {
        return new VerificationAnswer.Passed(name, PersonStatus.LOCAL, "MyKad", number, born, null, checkedAt);
    }

    public static VerificationAnswer.Passed aisyahsAnswer() {
        return localMyKad(HomaFixtures.AISYAH_NAME, AISYAH_MYKAD, LocalDate.of(1990, 3, 12), AISYAH_CHECKED_AT);
    }

    public static VerificationAnswer.Passed azmansAnswer() {
        return localMyKad("Azman", AZMAN_MYKAD, LocalDate.of(1979, 4, 5), AZMAN_CHECKED_AT);
    }

    public static VerificationAnswer.Passed sitisAnswer() {
        return localMyKad("Siti", SITI_MYKAD, ADULT_BORN, SITI_CHECKED_AT);
    }

    public static VerificationAnswer.Passed rinasAnswer() {
        return new VerificationAnswer.Passed("Rina", PersonStatus.FOREIGN, "i-Kad", RINA_IKAD, ADULT_BORN,
                RINA_IKAD_EXPIRY, RINA_CHECKED_AT);
    }

    /** Aisyah's son, aged 16 when he taps Reserve. */
    public static VerificationAnswer.Passed sonsAnswer() {
        return localMyKad("Aisyah's son", SON_MYKAD, LocalDate.of(2010, 6, 14), HomaFixtures.REQUESTED_AT);
    }

    public static VerificationAnswer.Passed danielsAnswer(Instant checkedAt) {
        return localMyKad("Daniel", DANIEL_MYKAD, LocalDate.of(1988, 8, 17), checkedAt);
    }
}
