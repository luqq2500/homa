package com.homa.verification.domain;

import com.homa.verification.domain.exception.DocumentNotSetForStatusException;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What "the country policy" means in one country (§14): who is local or foreign and the identity document each
 * verifies with (IDN-1, HLP-2, HLP-3), who may be an owner (PRV-1) or a customer (CUS-10), the business registry
 * (PRV-7), the privacy-notice languages (DAT-11), the personal data law (§10) and how fast a data request is
 * answered (DAT-12).
 *
 * <p>Every rule reads the policy; none names a country. A new country is a new instance, chosen in {@code app/},
 * with no rule changed. Payment record keeping (DAT-2) is Payment's value, and the currency is the kernel's
 * {@code Money}, so neither is held here.
 *
 * @param identityDocuments      every identity document the country verifies, each with its status
 * @param ownerStatuses          the statuses an owner may have (PRV-1)
 * @param customerStatuses       the statuses a customer may have (CUS-10)
 * @param privacyNoticeLanguages the languages each person can read and agree to the privacy notice in (DAT-11)
 * @param personalDataLaw        the law personal data is handled under (§10)
 * @param dataRequestAnswerTime  how long Support has to answer a request to see, correct or copy data (DAT-12)
 */
public record CountryPolicy(
        String country,
        List<AcceptedDocument> identityDocuments,
        Set<PersonStatus> ownerStatuses,
        Set<PersonStatus> customerStatuses,
        BusinessRegistry businessRegistry,
        List<NoticeLanguage> privacyNoticeLanguages,
        String personalDataLaw,
        Period dataRequestAnswerTime) {

    /** The identity documents Malaysia's policy names (§14). */
    public static final class Malaysia {

        /** Citizens' identity card. */
        public static final IdentityDocumentType MYKAD = new IdentityDocumentType("MyKad");
        /** Permanent residents' identity card. */
        public static final IdentityDocumentType MYPR = new IdentityDocumentType("MyPR");
        /** Foreign workers' card; it expires on or before the work permit, so its date is the permit's expiry. */
        public static final IdentityDocumentType IKAD = new IdentityDocumentType("i-Kad");

        private Malaysia() {
        }
    }

    /**
     * §14 for Malaysia. The 21 days to answer a data request is open question AG, accepted by the product owner
     * (docs/decisions/0003) as a country-policy item until the rules page states it.
     */
    public static final CountryPolicy MALAYSIA = new CountryPolicy(
            "Malaysia",
            List.of(
                    new AcceptedDocument(Malaysia.MYKAD, PersonStatus.LOCAL, "citizens", false),
                    new AcceptedDocument(Malaysia.MYPR, PersonStatus.LOCAL, "permanent residents", false),
                    new AcceptedDocument(Malaysia.IKAD, PersonStatus.FOREIGN, "foreign workers with a work permit", true)),
            Set.of(PersonStatus.LOCAL),
            Set.of(PersonStatus.LOCAL),
            new BusinessRegistry("Companies Commission of Malaysia", "SSM"),
            List.of(new NoticeLanguage("ms", "Bahasa Malaysia"), new NoticeLanguage("en", "English")),
            "Personal Data Protection Act 2010, as amended in 2024",
            Period.ofDays(21));

    public CountryPolicy {
        Objects.requireNonNull(country, "country");
        identityDocuments = List.copyOf(identityDocuments);
        ownerStatuses = Set.copyOf(ownerStatuses);
        customerStatuses = Set.copyOf(customerStatuses);
        Objects.requireNonNull(businessRegistry, "businessRegistry");
        privacyNoticeLanguages = List.copyOf(privacyNoticeLanguages);
        Objects.requireNonNull(personalDataLaw, "personalDataLaw");
        Objects.requireNonNull(dataRequestAnswerTime, "dataRequestAnswerTime");
    }

    /** IDN-1: the identity documents a person with {@code status} is verified with, in the policy's order. */
    public List<IdentityDocumentType> documentsFor(PersonStatus status) {
        return identityDocuments.stream()
                .filter(document -> document.status() == status)
                .map(AcceptedDocument::type)
                .toList();
    }

    /** The identity document the country calls {@code name}, if the policy names it. */
    public Optional<IdentityDocumentType> documentNamed(String name) {
        return identityDocuments.stream()
                .map(AcceptedDocument::type)
                .filter(type -> type.name().equals(name))
                .findFirst();
    }

    /**
     * Whether a person with {@code status} may hold {@code role}: a customer or an owner only with a status the
     * policy allows (CUS-10, PRV-1); a member as local or as foreign (HLP-2, HLP-3), since a foreign person is
     * verified only with a work permit's expiry.
     */
    public boolean allows(PersonRole role, PersonStatus status) {
        return switch (role) {
            case CUSTOMER -> customerStatuses.contains(status);
            case OWNER -> ownerStatuses.contains(status);
            case MEMBER -> true;
        };
    }

    /** IDN-1: the policy's row for {@code type}, refused unless the policy sets that document for {@code status}. */
    AcceptedDocument requireDocumentFor(PersonStatus status, IdentityDocumentType type) {
        return identityDocuments.stream()
                .filter(document -> document.status() == status && document.type().equals(type))
                .findFirst()
                .orElseThrow(() -> new DocumentNotSetForStatusException(country, status.name().toLowerCase(),
                        type.name()));
    }
}
