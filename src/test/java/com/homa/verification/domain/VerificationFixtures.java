package com.homa.verification.domain;

import static com.homa.fixtures.HomaFixtures.at;

import com.homa.fixtures.HomaFixtures;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The §0 people as Verification sees them, built from {@link HomaFixtures}: customer Aisyah, owner Azman of Kemas
 * Home, owner Hana of Hana Cleaning, local helpers Siti and Nurul, supervisor Farid, foreign helper Rina; also
 * Daniel from the CUS-10 case and Aisyah's 16-year-old son. Identity numbers are fictional. Nurul is a permanent
 * resident, so one local verifies with a MyPR.
 */
final class VerificationFixtures {

    private VerificationFixtures() {
    }

    static final CountryPolicy MALAYSIA = CountryPolicy.MALAYSIA;
    static final IdentityDocumentType MYKAD = CountryPolicy.Malaysia.MYKAD;
    static final IdentityDocumentType MYPR = CountryPolicy.Malaysia.MYPR;
    static final IdentityDocumentType IKAD = CountryPolicy.Malaysia.IKAD;
    static final IdentityDocumentType PASSPORT = new IdentityDocumentType("Passport");

    // Aisyah verifies at her first reservation, five minutes before she pays on Monday 30 November at 20:00 (CUS-3).
    static final PersonId AISYAH = new PersonId("person-aisyah");
    static final String AISYAH_MYKAD = "900312-10-5544";
    static final LocalDate AISYAH_BORN = LocalDate.of(1990, 3, 12);
    static final Instant AISYAH_CHECKED_AT = HomaFixtures.REQUESTED_AT.minus(Duration.ofMinutes(5));

    static final PersonId AISYAHS_SON = new PersonId("person-aisyahs-son");
    static final String AISYAHS_SON_MYKAD = "100614-10-5577";
    /** Aged 16 when he taps Reserve on Monday 30 November 2026. */
    static final LocalDate AISYAHS_SON_BORN = LocalDate.of(2010, 6, 14);

    static final PersonId DANIEL = new PersonId("person-daniel");
    static final PersonId DANIELS_SECOND_ACCOUNT = new PersonId("person-daniel-second-google-account");
    static final String DANIEL_MYKAD = "880817-14-6021";

    static final PersonId AZMAN = new PersonId("person-azman");
    static final String AZMAN_MYKAD = "790405-10-5123";

    static final PersonId HANA = new PersonId("person-hana");
    static final String HANA_MYKAD = "850923-08-6234";

    // Siti re-verified on Friday 20 November 2026 (MON-1, IDN-2).
    static final PersonId SITI = new PersonId("person-siti");
    static final String SITI_MYKAD = "960701-10-5246";
    static final Instant SITI_CHECKED_AT = at(2026, 11, 20, 10, 0);

    static final PersonId NURUL = new PersonId("person-nurul");
    static final String NURUL_MYPR = "981115-71-5032";

    static final PersonId FARID = new PersonId("person-farid");
    static final String FARID_MYKAD = "870228-10-5871";

    // Rina is verified on Monday 2 November 2026 with 200 days left on her permit (HLP-3).
    static final PersonId RINA = new PersonId("person-rina");
    static final String RINA_IKAD = "PW2611-0451";
    static final Instant RINA_CHECKED_AT = at(2026, 11, 2, 10, 0);
    static final LocalDate RINA_IKAD_EXPIRY = LocalDate.of(2026, 11, 2).plusDays(200);

    /** When Kemas Home adds members in the HLP-1 case: Monday 2 November 2026. */
    static final Instant MEMBER_CHECKED_AT = at(2026, 11, 2, 10, 0);

    static final LocalDate ADULT_BORN = LocalDate.of(1995, 1, 15);

    static IdentityCheck passedCheck(String name, PersonStatus status, IdentityDocumentType type, String number,
            LocalDate born, LocalDate documentExpiry, Instant checkedAt) {
        return new IdentityCheck(new PersonName(name), status, new IdentityDocument(type, new IdentityNumber(number)),
                born, documentExpiry, VerificationOutcome.PASSED, checkedAt);
    }

    static IdentityCheck localMyKadCheck(String name, String number, LocalDate born, Instant checkedAt) {
        return passedCheck(name, PersonStatus.LOCAL, MYKAD, number, born, null, checkedAt);
    }

    static IdentityCheck aisyahsCheck() {
        return localMyKadCheck(HomaFixtures.AISYAH_NAME, AISYAH_MYKAD, AISYAH_BORN, AISYAH_CHECKED_AT);
    }

    static IdentityCheck sitisCheck() {
        return localMyKadCheck("Siti", SITI_MYKAD, ADULT_BORN, SITI_CHECKED_AT);
    }

    static IdentityCheck rinasCheck() {
        return passedCheck("Rina", PersonStatus.FOREIGN, IKAD, RINA_IKAD, ADULT_BORN, RINA_IKAD_EXPIRY, RINA_CHECKED_AT);
    }

    static IdentityCheck azmansCheck() {
        return localMyKadCheck("Azman", AZMAN_MYKAD, LocalDate.of(1979, 4, 5), at(2026, 10, 1, 9, 0));
    }

    static IdentityCheck danielsCheck() {
        return localMyKadCheck("Daniel", DANIEL_MYKAD, LocalDate.of(1988, 8, 17), at(2026, 11, 24, 18, 0));
    }

    /** Verifies {@code id} for {@code role} under Malaysia's policy with a number no one holds yet. */
    static VerifiedPerson verified(PersonId id, PersonRole role, IdentityCheck check) {
        return VerifiedPerson.verify(id, role, check, MALAYSIA, IdentityNumberStatus.free());
    }

    static VerifiedPerson aisyah() {
        return verified(AISYAH, PersonRole.CUSTOMER, aisyahsCheck());
    }

    static VerifiedPerson siti() {
        return verified(SITI, PersonRole.MEMBER, sitisCheck());
    }

    static VerifiedPerson rina() {
        return verified(RINA, PersonRole.MEMBER, rinasCheck());
    }

    static VerifiedPerson azman() {
        return verified(AZMAN, PersonRole.OWNER, azmansCheck());
    }
}
