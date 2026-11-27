package com.homa.verification.domain;

import static com.homa.verification.domain.VerificationFixtures.IKAD;
import static com.homa.verification.domain.VerificationFixtures.MALAYSIA;
import static com.homa.verification.domain.VerificationFixtures.MYKAD;
import static com.homa.verification.domain.VerificationFixtures.MYPR;
import static com.homa.verification.domain.VerificationFixtures.PASSPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.verification.domain.exception.DocumentNotSetForStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks {@link CountryPolicy#MALAYSIA} row by row against the §14 table of docs/homa-business-rule.md, read from
 * the rules page itself, so the page and the code can't drift apart silently. A row added there fails
 * {@link #everyRowOfTheMalaysiaTableIsHeldByThePolicyOrOwnedElsewhere()} until it is checked here.
 */
class CountryPolicyTest {

    private static final Path RULES = Path.of("docs/homa-business-rule.md");

    /** The rows this policy holds; each has its own test below. */
    private static final Set<String> HELD = Set.of("Local", "Who can be an owner", "Who can be a customer",
            "Foreign", "Identity documents", "Business registration", "Privacy notice languages", "Personal data law");

    /** §14 rows another owner holds: Payment's record keeping (DAT-2), and the kernel's Money for the currency. */
    private static final Set<String> OWNED_ELSEWHERE = Set.of("Payment record keeping", "Currency");

    private static Map<String, String> malaysiaTable() throws IOException {
        List<String> lines = Files.readAllLines(RULES);
        int start = lines.indexOf("## 14. Country policy: Malaysia");
        assertThat(start).as("§14 heading in %s", RULES).isNotNegative();
        Map<String, String> rows = new LinkedHashMap<>();
        lines.subList(start + 1, lines.size()).stream()
                .takeWhile(line -> !line.startsWith("## "))
                .filter(line -> line.startsWith("| ") && !line.startsWith("| Item ") && !line.startsWith("| ---"))
                .map(line -> line.split("\\|"))
                .forEach(cells -> rows.put(cells[1].strip(), cells[2].strip()));
        return rows;
    }

    private static String cell(String item) throws IOException {
        return malaysiaTable().get(item);
    }

    private static String capitalised(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String statusesAsWritten(Set<PersonStatus> statuses) {
        List<String> names = new ArrayList<>();
        for (PersonStatus status : PersonStatus.values()) {
            if (statuses.contains(status)) {
                names.add(status.name().toLowerCase());
            }
        }
        String joined = String.join(" and ", names);
        return capitalised(names.size() == 1 ? joined + " only" : joined);
    }

    private static List<AcceptedDocument> documentsWithStatus(PersonStatus status) {
        return MALAYSIA.identityDocuments().stream().filter(document -> document.status() == status).toList();
    }

    @Test
    @Tag("§14")
    @DisplayName("Every row of the Malaysia table is held by the country policy, or by its owner elsewhere.")
    void everyRowOfTheMalaysiaTableIsHeldByThePolicyOrOwnedElsewhere() throws IOException {
        Set<String> rows = malaysiaTable().keySet();

        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(row -> assertThat(HELD.contains(row) || OWNED_ELSEWHERE.contains(row))
                .as("§14 row '%s' is neither held by CountryPolicy nor owned elsewhere", row).isTrue());
        assertThat(HELD).as("held rows no longer in §14").isSubsetOf(rows);
        assertThat(OWNED_ELSEWHERE).as("rows owned elsewhere no longer in §14").isSubsetOf(rows);
        assertThat(MALAYSIA.country()).isEqualTo("Malaysia");
    }

    @Test
    @Tag("HLP-2")
    @Tag("IDN-1")
    @DisplayName("Local means citizens (MyKad) and permanent residents (MyPR).")
    void localMeansCitizensWithMyKadAndPermanentResidentsWithMyPR() throws IOException {
        String asWritten = documentsWithStatus(PersonStatus.LOCAL).stream()
                .map(document -> document.holders() + " (" + document.type().name() + ")")
                .collect(Collectors.joining(" and "));

        assertThat(capitalised(asWritten)).isEqualTo(cell("Local"));
        assertThat(MALAYSIA.documentsFor(PersonStatus.LOCAL)).containsExactly(MYKAD, MYPR);
        assertThat(documentsWithStatus(PersonStatus.LOCAL)).noneMatch(AcceptedDocument::datesWorkPermit);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Who can be an owner: local only.")
    void onlyALocalCanBeAnOwner() throws IOException {
        assertThat(statusesAsWritten(MALAYSIA.ownerStatuses())).isEqualTo(cell("Who can be an owner"));
        assertThat(MALAYSIA.allows(PersonRole.OWNER, PersonStatus.LOCAL)).isTrue();
        assertThat(MALAYSIA.allows(PersonRole.OWNER, PersonStatus.FOREIGN)).isFalse();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Who can be a customer: local only.")
    void onlyALocalCanBeACustomer() throws IOException {
        assertThat(statusesAsWritten(MALAYSIA.customerStatuses())).isEqualTo(cell("Who can be a customer"));
        assertThat(MALAYSIA.allows(PersonRole.CUSTOMER, PersonStatus.LOCAL)).isTrue();
        assertThat(MALAYSIA.allows(PersonRole.CUSTOMER, PersonStatus.FOREIGN)).isFalse();
    }

    @Test
    @Tag("HLP-2")
    @Tag("HLP-3")
    @DisplayName("A member can be local, or foreign with a work permit.")
    void aMemberCanBeLocalOrForeign() {
        assertThat(MALAYSIA.allows(PersonRole.MEMBER, PersonStatus.LOCAL)).isTrue();
        assertThat(MALAYSIA.allows(PersonRole.MEMBER, PersonStatus.FOREIGN)).isTrue();
    }

    @Test
    @Tag("HLP-3")
    @Tag("IDN-1")
    @DisplayName("Foreign means foreign workers with a work permit, verified with their i-Kad, whose date is the permit's expiry.")
    void foreignMeansWorkersWithAPermitVerifiedWithTheirIKad() throws IOException {
        String foreign = cell("Foreign");
        List<AcceptedDocument> documents = documentsWithStatus(PersonStatus.FOREIGN);

        assertThat(documents).hasSize(1);
        AcceptedDocument ikad = documents.get(0);
        assertThat(foreign).startsWith(capitalised(ikad.holders()) + ", verified with their " + ikad.type().name() + ".");
        assertThat(ikad.datesWorkPermit()).isTrue();
        assertThat(foreign).contains("its date is used as the permit's expiry");
        assertThat(MALAYSIA.documentsFor(PersonStatus.FOREIGN)).containsExactly(IKAD);
    }

    @Test
    @Tag("IDN-1")
    @Tag("IDN-2")
    @DisplayName("The identity documents are MyKad, MyPR and i-Kad.")
    void theIdentityDocumentsAreMyKadMyPRAndIKad() throws IOException {
        String asWritten = MALAYSIA.identityDocuments().stream()
                .map(document -> document.type().name())
                .collect(Collectors.joining(", "));

        assertThat(asWritten).isEqualTo(cell("Identity documents"));
        assertThat(MALAYSIA.documentNamed("i-Kad")).contains(IKAD);
        assertThat(MALAYSIA.documentNamed("MyPR")).contains(MYPR);
        assertThat(MALAYSIA.documentNamed("Passport")).isEmpty();
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("A foreign candidate without an i-Kad can't be verified: a passport isn't a Malaysian identity document.")
    void aDocumentTheCountryDoesNotSetForTheStatusIsRefused() {
        assertThatThrownBy(() -> MALAYSIA.requireDocumentFor(PersonStatus.FOREIGN, PASSPORT))
                .isInstanceOf(DocumentNotSetForStatusException.class)
                .hasMessage("IDN-1: in Malaysia a foreign person isn't verified with Passport");
        assertThatThrownBy(() -> MALAYSIA.requireDocumentFor(PersonStatus.LOCAL, IKAD))
                .isInstanceOf(DocumentNotSetForStatusException.class)
                .hasMessage("IDN-1: in Malaysia a local person isn't verified with i-Kad");
        assertThat(MALAYSIA.requireDocumentFor(PersonStatus.LOCAL, MYPR).holders()).isEqualTo("permanent residents");
    }

    @Test
    @Tag("PRV-7")
    @DisplayName("A Business provider's business is verified with the Companies Commission of Malaysia (SSM).")
    void businessesRegisterWithTheCompaniesCommissionOfMalaysia() throws IOException {
        BusinessRegistry registry = MALAYSIA.businessRegistry();

        assertThat(registry.name() + " (" + registry.acronym() + ")").isEqualTo(cell("Business registration"));
        assertThat(registry.acronym()).isEqualTo("SSM");
    }

    @Test
    @Tag("DAT-11")
    @DisplayName("Nurul reads and agrees to the privacy notice herself, in Bahasa Malaysia or English.")
    void thePrivacyNoticeIsInBahasaMalaysiaAndEnglish() throws IOException {
        String asWritten = MALAYSIA.privacyNoticeLanguages().stream()
                .map(NoticeLanguage::name)
                .collect(Collectors.joining(" and "));

        assertThat(asWritten).isEqualTo(cell("Privacy notice languages"));
        assertThat(MALAYSIA.privacyNoticeLanguages()).extracting(NoticeLanguage::tag).containsExactly("ms", "en");
    }

    @Test
    @Tag("§10")
    @DisplayName("Personal data is handled under the Personal Data Protection Act 2010, as amended in 2024.")
    void personalDataFollowsThePersonalDataProtectionAct() throws IOException {
        assertThat(MALAYSIA.personalDataLaw()).isEqualTo(cell("Personal data law"));
    }

    @Test
    @Tag("DAT-12")
    @DisplayName("Siti asks for a copy of her data; Support answers within 21 days (open question AG, accepted).")
    void aDataRequestIsAnsweredWithin21Days() {
        assertThat(MALAYSIA.dataRequestAnswerTime()).isEqualTo(Period.ofDays(21));
    }

    @Test
    @Tag("§14")
    @Tag("IDN-1")
    @DisplayName("A new country needs only its own policy: the same rules verify its people with its own documents.")
    void aNewCountryIsOnlyANewPolicy() {
        IdentityDocumentType nationalCard = new IdentityDocumentType("National card");
        IdentityDocumentType workPass = new IdentityDocumentType("Work pass");
        List<AcceptedDocument> documents = new ArrayList<>(List.of(
                new AcceptedDocument(nationalCard, PersonStatus.LOCAL, "citizens", false),
                new AcceptedDocument(workPass, PersonStatus.FOREIGN, "foreign workers", true)));
        CountryPolicy elsewhere = new CountryPolicy("Elsewhere", documents,
                Set.of(PersonStatus.LOCAL, PersonStatus.FOREIGN), Set.of(PersonStatus.LOCAL),
                new BusinessRegistry("Elsewhere Registry", "ER"), List.of(new NoticeLanguage("en", "English")),
                "Elsewhere Data Act", Period.ofDays(30));
        documents.clear();

        assertThat(elsewhere.documentsFor(PersonStatus.LOCAL)).containsExactly(nationalCard);
        assertThat(elsewhere.allows(PersonRole.OWNER, PersonStatus.FOREIGN)).isTrue();
        assertThat(elsewhere.allows(PersonRole.CUSTOMER, PersonStatus.FOREIGN)).isFalse();
        assertThat(elsewhere.documentNamed("MyKad")).isEmpty();
    }
}
