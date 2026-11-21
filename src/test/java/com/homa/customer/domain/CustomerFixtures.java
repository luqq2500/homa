package com.homa.customer.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_NAME;

import com.homa.fixtures.HomaFixtures;
import java.time.Instant;

/**
 * The §0 customer, Aisyah in Salak Tinggi, built as Customer domain objects from {@link HomaFixtures}. Also Daniel,
 * the customer Support suspends in the CUS-4, CUS-6, CUS-7 and CUS-9 cases.
 */
final class CustomerFixtures {

    private CustomerFixtures() {
    }

    static final String GOOGLE = "https://accounts.google.com";
    static final String APPLE = "https://appleid.apple.com";

    static final String AISYAH_GOOGLE_SUBJECT = "108204268033311374519";
    static final String AISYAH_STREET_ADDRESS = "B-12-3, Residensi Salak, Jalan Salak 1";
    static final String AISYAH_ARRIVAL_NOTE = "Guardhouse: say unit B-12-3";
    /** Aisyah agrees to the Terms of Service when she registers, a week before she reserves Kemas Home. */
    static final Instant AISYAH_AGREED_AT = HomaFixtures.at(2026, 11, 23, 21, 0);

    static final String DANIEL_GOOGLE_SUBJECT = "117730221843660915602";
    static final String DANIEL_NAME = "Daniel";

    static final String SAFETY_AND_MISCONDUCT =
            "Confirmed Safety & Misconduct report: shouted at and threatened Kemas Home's helpers";
    /** Support confirms Kemas Home's report and suspends Daniel on Wednesday 2 December at 15:00. */
    static final Instant DANIEL_SUSPENDED_AT = HomaFixtures.at(2026, 12, 2, 15, 0);

    static SignInId aisyahsGoogleAccount() {
        return new SignInId(GOOGLE, AISYAH_GOOGLE_SUBJECT);
    }

    static SignInId danielsGoogleAccount() {
        return new SignInId(GOOGLE, DANIEL_GOOGLE_SUBJECT);
    }

    static CustomerName aisyahsName() {
        return new CustomerName(AISYAH_NAME);
    }

    static CityCode salakTinggi() {
        return new CityCode(AISYAH_CITY);
    }

    static HomeAddress aisyahsHome() {
        return new HomeAddress(AISYAH_STREET_ADDRESS, salakTinggi());
    }

    static ArrivalNote aisyahsArrivalNote() {
        return new ArrivalNote(AISYAH_ARRIVAL_NOTE);
    }

    static TermsAcceptance aisyahsTermsAcceptance() {
        return TermsAcceptance.of(true, AISYAH_AGREED_AT);
    }

    /**
     * Aisyah, signed in with her Google account, which held no Homa account yet (CUS-7), registered with every
     * detail CUS-1 asks for and the Terms of Service agreed (CUS-2).
     */
    static Customer aisyah() {
        return Customer.register(aisyahsGoogleAccount(), SignInStatus.free(), aisyahsName(), aisyahsHome(),
                aisyahsArrivalNote(), aisyahsTermsAcceptance());
    }

    static HomeAddress danielsHome() {
        return new HomeAddress("12, Jalan Merbuk 3", salakTinggi());
    }

    /** Daniel registering with {@code signIn}, whose standing the caller looked up as {@code status}. */
    static Customer danielRegistering(SignInId signIn, SignInStatus status) {
        return Customer.register(signIn, status, new CustomerName(DANIEL_NAME), danielsHome(),
                new ArrivalNote("Corner house, blue gate"), aisyahsTermsAcceptance());
    }

    /** Daniel, an active customer living in Salak Tinggi like Aisyah, signed in with his Google account. */
    static Customer daniel() {
        return danielRegistering(danielsGoogleAccount(), SignInStatus.free());
    }

    /** Daniel, suspended by Support after the confirmed Safety &amp; Misconduct report (CUS-4). */
    static Customer danielSuspended(SuspensionPeriod period) {
        Customer daniel = daniel();
        daniel.suspend(period, SAFETY_AND_MISCONDUCT, DANIEL_SUSPENDED_AT);
        return daniel;
    }
}
