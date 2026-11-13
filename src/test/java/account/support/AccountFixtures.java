package account.support;

import account.domain.Address;
import account.domain.BusinessAccount;
import account.domain.CustomerAccount;
import account.domain.Language;
import account.domain.PhoneNumber;
import account.domain.PhoneVerification;
import java.time.Instant;
import kernel.City;
import kernel.GeoPoint;

/** Ready-made account objects for tests. */
public final class AccountFixtures {

    public static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");
    public static final PhoneNumber PHONE = new PhoneNumber("012-345 6789");
    public static final String CODE = "482913";
    public static final GeoPoint PIN = new GeoPoint(2.9213, 101.6559);
    public static final Address HOME = new Address("12 Jalan Teknokrat 5", PIN, City.CYBERJAYA);

    private AccountFixtures() {
    }

    /** A verification whose code was confirmed, ready to register an account. */
    public static PhoneVerification verified(PhoneNumber phone) {
        PhoneVerification verification = PhoneVerification.send(phone, CODE, NOW);
        verification.confirm(CODE, NOW);
        return verification;
    }

    public static CustomerAccount customer() {
        return CustomerAccount.register(verified(PHONE), "Aisyah binti Rahman", HOME, Language.ENGLISH);
    }

    public static BusinessAccount business() {
        return BusinessAccount.register(verified(PHONE), Language.MALAY);
    }
}
