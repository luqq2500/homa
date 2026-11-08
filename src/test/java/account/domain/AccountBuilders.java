package account.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Test-data builders for accounts, read as domain sentences. */
final class AccountBuilders {

    static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    static final LocalDate SATURDAY = LocalDate.of(2026, 11, 14);
    static final PhoneNumber PHONE = PhoneNumber.of("+60123456789");
    static final String CODE = "482913";

    private AccountBuilders() {
    }

    /** A time on Saturday, written as "10:00". */
    static Instant at(String time) {
        return SATURDAY.atTime(LocalTime.parse(time)).atZone(MALAYSIA).toInstant();
    }

    static PhoneVerification aPhoneVerificationStartedAt(String time) {
        return PhoneVerification.start(PHONE, CODE, at(time));
    }

    static PhoneVerification aConfirmedPhoneVerification() {
        PhoneVerification verification = aPhoneVerificationStartedAt("10:00");
        verification.confirm(CODE, at("10:01"));
        return verification;
    }

    static PhoneVerification anUnconfirmedPhoneVerification() {
        return aPhoneVerificationStartedAt("10:00");
    }

    static Address anAddressIn(String city) {
        return Address.of("12 Jalan Teknokrat 3", City.of(city), Coordinates.of(2.9213, 101.6559));
    }

    static ProviderAccount aProvider() {
        return ProviderAccount.register(aConfirmedPhoneVerification(), "Nur Aina binti Ahmad");
    }

    static Helper aHelperNamed(String name) {
        return Helper.invite("provider-1", name, Photo.of("photos/" + name + ".jpg"));
    }
}
