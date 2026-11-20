package com.homa.customer.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_NAME;

import com.homa.fixtures.HomaFixtures;
import java.time.Instant;

/**
 * The §0 customer, Aisyah in Salak Tinggi, built as Customer domain objects from {@link HomaFixtures}. Also Daniel,
 * the customer Support suspends in the CUS-4 and CUS-6 cases.
 */
final class CustomerFixtures {

    private CustomerFixtures() {
    }

    static final String AISYAH_PHONE = "+60123456789";
    static final String AISYAH_STREET_ADDRESS = "B-12-3, Residensi Salak, Jalan Salak 1";
    static final String AISYAH_ARRIVAL_NOTE = "Guardhouse: say unit B-12-3";
    /** Aisyah agrees to the Terms of Service when she registers, a week before she reserves Kemas Home. */
    static final Instant AISYAH_AGREED_AT = HomaFixtures.at(2026, 11, 23, 21, 0);

    static final String DANIEL_PHONE = "+60198765432";
    static final String DANIEL_NAME = "Daniel";

    static VerifiedPhone aisyahsVerifiedPhone() {
        return new VerifiedPhone(AISYAH_PHONE);
    }

    static UserName aisyahsUserName() {
        return new UserName(AISYAH_NAME);
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

    /** Aisyah, registered with every detail CUS-1 asks for and the Terms of Service agreed (CUS-2). */
    static Customer aisyah() {
        return Customer.register(aisyahsVerifiedPhone(), aisyahsUserName(), aisyahsHome(), aisyahsArrivalNote(),
                aisyahsTermsAcceptance());
    }

    /** Daniel, an active customer living in Salak Tinggi like Aisyah. */
    static Customer daniel() {
        return Customer.register(new VerifiedPhone(DANIEL_PHONE), new UserName(DANIEL_NAME),
                new HomeAddress("12, Jalan Merbuk 3", salakTinggi()), new ArrivalNote("Corner house, blue gate"),
                aisyahsTermsAcceptance());
    }
}
