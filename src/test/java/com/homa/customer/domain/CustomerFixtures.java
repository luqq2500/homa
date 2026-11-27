package com.homa.customer.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_NAME;

import com.homa.fixtures.HomaFixtures;
import java.time.Instant;

/**
 * The §0 customer, Aisyah in Salak Tinggi, who is identity verified, built as Customer domain objects from
 * {@link HomaFixtures}. Also her husband and her 16-year-old son (CUS-3, CUS-10), and Daniel, the customer Support
 * suspends in the CUS-4 to CUS-7, CUS-10 and DAT-7 cases.
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
    /** The personId Verification gives Aisyah when she verifies with her MyKad. */
    static final String AISYAH_PERSON_ID = "person-aisyah";

    static final String HUSBAND_GOOGLE_SUBJECT = "102938475610293847561";
    static final String HUSBAND_NAME = "Hakim";
    static final String HUSBAND_PERSON_ID = "person-hakim";

    static final String SON_GOOGLE_SUBJECT = "113355779911335577991";
    static final String SON_PERSON_ID = "person-aisyahs-son";

    static final String DANIEL_GOOGLE_SUBJECT = "117730221843660915602";
    static final String DANIEL_NAME = "Daniel";
    static final String DANIEL_PERSON_ID = "person-daniel";

    static final String SAFETY_AND_MISCONDUCT =
            "Confirmed Safety & Misconduct report: threatened Kemas Home's helpers";
    /** Support confirms Kemas Home's report and suspends Daniel on Wednesday 2 December at 15:00. */
    static final Instant DANIEL_SUSPENDED_AT = HomaFixtures.at(2026, 12, 2, 15, 0);
    /** The end Support works out for a 7-day suspension from Daniel's: Wednesday 9 December at 15:00. */
    static final Instant SEVEN_DAYS_END = HomaFixtures.at(2026, 12, 9, 15, 0);
    /** The end Support works out for a 30-day suspension from Daniel's: Friday 1 January 2027 at 15:00. */
    static final Instant THIRTY_DAYS_END = HomaFixtures.at(2027, 1, 1, 15, 0);

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

    /** What Verification finds when Aisyah verifies with her MyKad: local and aged 18 or above. */
    static VerifiedIdentity aisyahsMyKad() {
        return new VerifiedIdentity(new PersonId(AISYAH_PERSON_ID), true, true);
    }

    /**
     * Aisyah, signed in with her Google account, which held no Homa account yet (CUS-7), registered with every
     * detail CUS-1 asks for and the Terms of Service agreed (CUS-2). She hasn't verified her identity yet.
     */
    static Customer aisyahJustRegistered() {
        return Customer.register(aisyahsGoogleAccount(), SignInStatus.free(), aisyahsName(), aisyahsHome(),
                aisyahsArrivalNote(), aisyahsTermsAcceptance());
    }

    /** The §0 Aisyah: registered and identity verified with her MyKad (CUS-3, CUS-10). */
    static Customer aisyah() {
        Customer aisyah = aisyahJustRegistered();
        aisyah.recordIdentityVerified(aisyahsMyKad(), IdentityStatus.free());
        return aisyah;
    }

    /** Aisyah's husband, registered with his own Google account at their Salak Tinggi home, not yet verified. */
    static Customer aisyahsHusband() {
        return Customer.register(new SignInId(GOOGLE, HUSBAND_GOOGLE_SUBJECT), SignInStatus.free(),
                new CustomerName(HUSBAND_NAME), aisyahsHome(), aisyahsArrivalNote(), aisyahsTermsAcceptance());
    }

    static VerifiedIdentity husbandsMyKad() {
        return new VerifiedIdentity(new PersonId(HUSBAND_PERSON_ID), true, true);
    }

    /** Aisyah's son, registered with his own Google account at their Salak Tinggi home, not yet verified. */
    static Customer aisyahsSon() {
        return Customer.register(new SignInId(GOOGLE, SON_GOOGLE_SUBJECT), SignInStatus.free(),
                new CustomerName("Aisyah's son"), aisyahsHome(), aisyahsArrivalNote(), aisyahsTermsAcceptance());
    }

    /** What Verification finds when Aisyah's son verifies with his MyKad: local, but {@code ageCheckPassed}. */
    static VerifiedIdentity sonsMyKad(boolean ageCheckPassed) {
        return new VerifiedIdentity(new PersonId(SON_PERSON_ID), true, ageCheckPassed);
    }

    static HomeAddress danielsHome() {
        return new HomeAddress("12, Jalan Merbuk 3", salakTinggi());
    }

    /** Daniel registering with {@code signIn}, whose standing the caller looked up as {@code status}. */
    static Customer danielRegistering(SignInId signIn, SignInStatus status) {
        return Customer.register(signIn, status, new CustomerName(DANIEL_NAME), danielsHome(),
                new ArrivalNote("Corner house, blue gate"), aisyahsTermsAcceptance());
    }

    /** What Verification finds when Daniel verifies with his MyKad: local and aged 18 or above. */
    static VerifiedIdentity danielsMyKad() {
        return new VerifiedIdentity(new PersonId(DANIEL_PERSON_ID), true, true);
    }

    /**
     * Daniel, an active customer living in Salak Tinggi like Aisyah, signed in with his Google account and identity
     * verified with his MyKad.
     */
    static Customer daniel() {
        Customer daniel = danielRegistering(danielsGoogleAccount(), SignInStatus.free());
        daniel.recordIdentityVerified(danielsMyKad(), IdentityStatus.free());
        return daniel;
    }

    /**
     * The suspension Support decides for Daniel after the confirmed Safety &amp; Misconduct report (CUS-4), with
     * the end Support works out for {@code length}.
     */
    static Suspension supportSuspendsDaniel(SuspensionLength length) {
        Instant endsAt = switch (length) {
            case SEVEN_DAYS -> SEVEN_DAYS_END;
            case THIRTY_DAYS -> THIRTY_DAYS_END;
            case UNTIL_LIFTED -> null;
        };
        return Suspension.decided(length, endsAt, SAFETY_AND_MISCONDUCT, DANIEL_SUSPENDED_AT);
    }

    /** Daniel, suspended by Support after the confirmed Safety &amp; Misconduct report (CUS-4). */
    static Customer danielSuspended(SuspensionLength length) {
        Customer daniel = daniel();
        daniel.suspend(supportSuspendsDaniel(length));
        return daniel;
    }
}
