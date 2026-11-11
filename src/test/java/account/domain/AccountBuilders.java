package account.domain;

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
    static final BusinessRegistration KEMAS = new BusinessRegistration("202601012345", "Kemas Home Care Sdn Bhd");

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

    /** A business provider, registered and not yet verified. */
    static ProviderAccount aProvider() {
        return ProviderAccount.register(aConfirmedPhoneVerification());
    }

    /** Kemas's KYB passed, verified until {@code day}. */
    static KybResult kemasVerifiedUntil(String day) {
        return KybResult.verified(KEMAS, on(day));
    }

    /** A business verified on 1 January, until 1 April. */
    static ProviderAccount aVerifiedProvider() {
        ProviderAccount provider = aProvider();
        provider.recordKybResult(kemasVerifiedUntil("1 April"), on("1 January"));
        return provider;
    }

    static EmploymentConfirmation confirmedOn(String day) {
        return EmploymentConfirmation.given("2026-11", on(day));
    }

    /** Aina's KYC passed, verified until {@code day}. */
    static KycResult ainaVerifiedUntil(String day) {
        return KycResult.verified("Aina binti Ahmad", on(day));
    }

    /** The provider starts adding a helper on {@code day}, confirming lawful employment. */
    static MemberVerification aHelperWaitingSince(String day) {
        return MemberVerification.start("provider-1", TeamRole.HELPER, Photo.of("photos/aina.jpg"), confirmedOn(day), on(day));
    }

    /** Aina, a helper added and verified on 1 January, until 1 April. */
    static TeamMember aina() {
        MemberVerification verification = aHelperWaitingSince("1 January");
        verification.recordKycResult(ainaVerifiedUntil("1 April"), on("1 January"));
        return TeamMember.add(verification, on("1 January"));
    }
}
