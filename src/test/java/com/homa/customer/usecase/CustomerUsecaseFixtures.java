package com.homa.customer.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_NAME;

import com.homa.customer.domain.ArrivalNote;
import com.homa.customer.domain.CityCode;
import com.homa.customer.domain.Customer;
import com.homa.customer.domain.CustomerName;
import com.homa.customer.domain.HomeAddress;
import com.homa.customer.domain.IdentityStatus;
import com.homa.customer.domain.PersonId;
import com.homa.customer.domain.SignInId;
import com.homa.customer.domain.SignInStatus;
import com.homa.customer.domain.TermsAcceptance;
import com.homa.customer.domain.VerifiedIdentity;
import com.homa.customer.usecase.dto.RegisterAccountRequest;
import com.homa.customer.usecase.dto.UpdateProfileRequest;
import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.PersonVerifiedIE;
import java.time.Instant;

/**
 * The §0 customer Aisyah and the other customers of the CUS cases, as the requests and events the Customer use
 * cases receive, built from {@link HomaFixtures}.
 */
final class CustomerUsecaseFixtures {

    private CustomerUsecaseFixtures() {
    }

    static final String GOOGLE = "https://accounts.google.com";
    static final String APPLE = "https://appleid.apple.com";

    static final String AISYAH_GOOGLE_SUBJECT = "108204268033311374519";
    static final String AISYAH_STREET_ADDRESS = "B-12-3, Residensi Salak, Jalan Salak 1";
    static final String AISYAH_ARRIVAL_NOTE = "Guardhouse: say unit B-12-3";
    static final String AISYAH_PERSON_ID = "person-aisyah";

    static final String CYBERJAYA = "CYBERJAYA";
    static final String CYBERJAYA_STREET_ADDRESS = "A-3-1, Residensi Cyber, Persiaran Multimedia";
    static final String CYBERJAYA_ARRIVAL_NOTE = "Lobby: ask the concierge for unit A-3-1";

    static final String DANIEL_GOOGLE_SUBJECT = "117730221843660915602";
    static final String DANIEL_SECOND_GOOGLE_SUBJECT = "100200300400500600700";
    static final String DANIEL_PERSON_ID = "person-daniel";

    static final String SON_GOOGLE_SUBJECT = "113355779911335577991";
    static final String SON_PERSON_ID = "person-aisyahs-son";

    /** Aisyah registers on Monday 23 November at 21:00, a week before she reserves Kemas Home. */
    static final Instant REGISTERED_AT = HomaFixtures.at(2026, 11, 23, 21, 0);
    /** She verifies with her MyKad when she first taps Reserve, before paying (CUS-3). */
    static final Instant VERIFIED_AT = HomaFixtures.at(2026, 11, 30, 19, 55);
    /** Customer hears of the verification a moment later. */
    static final Instant HEARD_AT = HomaFixtures.at(2026, 11, 30, 19, 56);

    /** Aisyah's registration: her Google sign-in, name, Salak Tinggi home and guardhouse note, Terms ticked. */
    static RegisterAccountRequest aisyahRegisters() {
        return aisyahRegisters(true);
    }

    static RegisterAccountRequest aisyahRegisters(boolean termsOfServiceAgreed) {
        return new RegisterAccountRequest(GOOGLE, AISYAH_GOOGLE_SUBJECT, AISYAH_NAME, AISYAH_STREET_ADDRESS,
                AISYAH_CITY, AISYAH_ARRIVAL_NOTE, termsOfServiceAgreed);
    }

    /** Aisyah, registered with her Google account and not yet verified. */
    static Customer aisyahJustRegistered() {
        return Customer.register(new SignInId(GOOGLE, AISYAH_GOOGLE_SUBJECT), SignInStatus.free(),
                new CustomerName(AISYAH_NAME), new HomeAddress(AISYAH_STREET_ADDRESS, new CityCode(AISYAH_CITY)),
                new ArrivalNote(AISYAH_ARRIVAL_NOTE), TermsAcceptance.of(true, REGISTERED_AT));
    }

    /** A customer registered with {@code subject}'s Google account at a Salak Tinggi home, not yet verified. */
    static Customer registeredWithGoogle(String subject, String name) {
        return Customer.register(new SignInId(GOOGLE, subject), SignInStatus.free(), new CustomerName(name),
                new HomeAddress("12, Jalan Merbuk 3", new CityCode(AISYAH_CITY)),
                new ArrivalNote("Corner house, blue gate"), TermsAcceptance.of(true, REGISTERED_AT));
    }

    /** {@code customer}, already verified as local and aged 18 or above with {@code personId}'s MyKad. */
    static Customer verifiedAs(Customer customer, String personId) {
        customer.recordIdentityVerified(new VerifiedIdentity(new PersonId(personId), true, true),
                IdentityStatus.free());
        return customer;
    }

    /** Aisyah saves her profile after moving to Cyberjaya. */
    static UpdateProfileRequest aisyahMovesToCyberjaya(String customerId) {
        return new UpdateProfileRequest(customerId, customerId, "Aisyah R.", CYBERJAYA_STREET_ADDRESS, CYBERJAYA,
                CYBERJAYA_ARRIVAL_NOTE);
    }

    /** What Verification announces when a customer account verifies with a local MyKad. */
    static PersonVerifiedIE customerVerified(String eventId, String personId, String accountId,
            boolean ageCheckPassed) {
        return new PersonVerifiedIE(eventId, personId, "CUSTOMER", accountId, AISYAH_NAME, "LOCAL",
                ageCheckPassed, null, null, VERIFIED_AT);
    }

    /** Aisyah's MyKad verification for {@code accountId}: local and aged 18 or above. */
    static PersonVerifiedIE aisyahsMyKad(String eventId, String accountId) {
        return customerVerified(eventId, AISYAH_PERSON_ID, accountId, true);
    }
}
