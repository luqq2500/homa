package account.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Test-data builders for accounts, read as domain sentences. */
final class AccountBuilders {

    static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    static final LocalDate SATURDAY = LocalDate.of(2026, 11, 14);
    static final PhoneNumber PHONE = PhoneNumber.of("+60123456789");
    static final String CODE = "482913";
    static final VerificationLimits LIMITS =
            new VerificationLimits(3, Duration.ofDays(14), Duration.ofDays(14), Duration.ofHours(1));
    static final Country INDONESIA = Country.of("ID");

    /** The launch list: Malaysian documents for providers and independent members, any country for business members. */
    static final AcceptedDocuments ACCEPTED = new AcceptedDocuments(Set.of(
            accepted(DocumentHolder.PROVIDER, DocumentType.IDENTITY_CARD, Optional.of(Country.MALAYSIA)),
            accepted(DocumentHolder.PROVIDER, DocumentType.PASSPORT, Optional.of(Country.MALAYSIA)),
            accepted(DocumentHolder.INDEPENDENT_TEAM_MEMBER, DocumentType.IDENTITY_CARD, Optional.of(Country.MALAYSIA)),
            accepted(DocumentHolder.INDEPENDENT_TEAM_MEMBER, DocumentType.PASSPORT, Optional.of(Country.MALAYSIA)),
            accepted(DocumentHolder.BUSINESS_TEAM_MEMBER, DocumentType.IDENTITY_CARD, Optional.empty()),
            accepted(DocumentHolder.BUSINESS_TEAM_MEMBER, DocumentType.PASSPORT, Optional.empty())));

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

    private static AcceptedDocument accepted(DocumentHolder holder, DocumentType type, Optional<Country> country) {
        return new AcceptedDocument(holder, type, country);
    }

    static IdentityDocument aMyKad(String registeredName) {
        return new IdentityDocument(Country.MALAYSIA, DocumentType.IDENTITY_CARD, registeredName);
    }

    static EkycResult verifiedWith(IdentityDocument document) {
        return EkycResult.verified(document);
    }

    /** Verified with a MyKad in the name "Aina binti Ahmad". */
    static EkycResult verifiedAsAina() {
        return verifiedWith(aMyKad("Aina binti Ahmad"));
    }

    static EmploymentConfirmation confirmedOn(String day) {
        return EmploymentConfirmation.given("2026-11", on(day));
    }

    static ProviderAccount aProvider() {
        return ProviderAccount.register(aConfirmedPhoneVerification(), ProviderType.INDEPENDENT);
    }

    static ProviderAccount aBusinessProvider() {
        return ProviderAccount.register(aConfirmedPhoneVerification(), ProviderType.BUSINESS);
    }

    /** An independent provider first verified on {@code day} with a MyKad. */
    static ProviderAccount aProviderVerifiedOn(String day) {
        ProviderAccount provider = aProvider();
        provider.recordEkycResult(verifiedWith(aMyKad("Nur Aina binti Ahmad")), ACCEPTED, LIMITS, on(day));
        return provider;
    }

    /** A provider of {@code type} starts adding a person on {@code day}, confirming lawful employment. */
    static MemberVerification startAdding(ProviderType type, TeamRole role, DocumentType document, Country country, String day) {
        return MemberVerification.start(
                "provider-1", type, role, document, country, Photo.of("photos/aina.jpg"), confirmedOn(day), ACCEPTED, LIMITS, on(day));
    }

    /** An independent provider starts adding a helper with a MyKad on {@code day}. */
    static MemberVerification aHelperWaitingSince(String day) {
        return startAdding(ProviderType.INDEPENDENT, TeamRole.HELPER, DocumentType.IDENTITY_CARD, Country.MALAYSIA, day);
    }

    /** A helper with a MyKad, verified and added on {@code day}. */
    static TeamMember aTeamMemberSince(String day) {
        MemberVerification verification = aHelperWaitingSince(day);
        verification.recordEkycResult(verifiedAsAina(), ACCEPTED, on(day));
        return TeamMember.add(verification, on(day));
    }
}
