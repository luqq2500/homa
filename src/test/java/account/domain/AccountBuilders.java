package account.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Test-data builders for accounts, read as domain sentences. */
final class AccountBuilders {

    static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    static final LocalDate SATURDAY = LocalDate.of(2026, 11, 14);
    static final PhoneNumber PHONE = PhoneNumber.of("+60123456789");
    static final String CODE = "482913";
    static final VerificationLimits LIMITS = new VerificationLimits(3, Duration.ofDays(14), Duration.ofDays(14));

    private AccountBuilders() {
    }

    /** 10:00 in Malaysia on a day of 2027, written as "1 January". */
    static Instant on(String day) {
        return LocalDate.parse(day + " 2027", DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
                .atTime(10, 0)
                .atZone(MALAYSIA)
                .toInstant();
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

    /** A helper who became a team member on {@code day}. */
    static Helper aTeamMemberSince(String day) {
        Helper helper = aHelperNamed("Aina");
        helper.recordEkycResult(VerificationStatus.VERIFIED, LIMITS, on(day));
        return helper;
    }

    /** A provider first verified on {@code day}. */
    static ProviderAccount aProviderVerifiedOn(String day) {
        ProviderAccount provider = aProvider();
        provider.recordEkycResult(VerificationStatus.VERIFIED, LIMITS, on(day));
        return provider;
    }
}
