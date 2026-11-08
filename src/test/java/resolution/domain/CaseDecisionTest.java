package resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import kernel.Money;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import resolution.domain.exception.CaseAlreadyDecidedException;
import resolution.domain.exception.DecisionReasonRequiredException;
import resolution.domain.exception.InvalidDecisionAmountException;
import resolution.domain.exception.ResolutionDetailRequiredException;

/** Feature: Deciding a case. */
class CaseDecisionTest {

    private static final Instant OPENED = Instant.parse("2026-11-14T06:00:00Z");
    private static final Instant DECIDED = Instant.parse("2026-11-14T10:00:00Z");
    private static final EvidenceRef A_PHOTO = EvidenceRef.of("evidence/kitchen-after.jpg");

    private static Money rm(String amount) {
        return Money.parse(amount);
    }

    private static Case anOpenCase(CaseSource source) {
        return Case.open("reservation-1", source, List.of(A_PHOTO), rm("200.00"), OPENED);
    }

    private static Decision decision(String name) {
        return switch (name) {
            case "release" -> Decision.release();
            case "full refund" -> Decision.fullRefund();
            default -> Decision.partialRefund(rm("100.00"));
        };
    }

    @Nested
    class Opening {

        @Test
        void aNewCaseIsOpenWithItsEvidence() {
            Case opened = anOpenCase(CaseSource.CUSTOMER_REPORT);

            assertThat(opened.isOpen()).isTrue();
            assertThat(opened.status()).isEqualTo(CaseStatus.OPEN);
            assertThat(opened.reservationId()).isEqualTo("reservation-1");
            assertThat(opened.source()).isEqualTo(CaseSource.CUSTOMER_REPORT);
            assertThat(opened.atStake()).isEqualTo(rm("200.00"));
            assertThat(opened.openedAt()).isEqualTo(OPENED);
            assertThat(opened.evidence()).containsExactly(A_PHOTO);
            assertThat(opened.id()).isNotBlank();
            assertThat(opened.decision()).isEmpty();
            assertThat(opened.reason()).isEmpty();
            assertThat(opened.decidedAt()).isEmpty();
        }

        @Test
        void moreEvidenceCanBeAddedWhileTheCaseIsOpen() {
            Case opened = anOpenCase(CaseSource.CUSTOMER_REPORT);

            opened.addEvidence(List.of(EvidenceRef.of("evidence/provider-video.mp4")));

            assertThat(opened.evidence()).hasSize(2);
        }

        @Test
        void aCaseIsAboutAReservation() {
            assertThatThrownBy(() -> Case.open(" ", CaseSource.CUSTOMER_REPORT, List.of(), rm("200"), OPENED))
                    .isInstanceOf(ResolutionDetailRequiredException.class);
            assertThatThrownBy(() -> Case.open(null, CaseSource.CUSTOMER_REPORT, List.of(), rm("200"), OPENED))
                    .isInstanceOf(ResolutionDetailRequiredException.class);
            assertThatThrownBy(() -> EvidenceRef.of(" ")).isInstanceOf(ResolutionDetailRequiredException.class);
            assertThat(EvidenceRef.of(" check-in ").reference()).isEqualTo("check-in");
        }
    }

    @Nested
    class Deciding {

        @ParameterizedTest(name = "a decision with reason \"{0}\" is rejected")
        @NullSource
        @ValueSource(strings = {"", "  "})
        void aDecisionNeedsAReason(String reason) {
            Case opened = anOpenCase(CaseSource.CUSTOMER_REPORT);

            assertThatThrownBy(() -> opened.decide(Decision.release(), reason, DECIDED))
                    .isInstanceOf(DecisionReasonRequiredException.class);
            assertThat(opened.isOpen()).isTrue();
        }

        @Test
        void aCaseIsDecidedOnce() {
            Case decided = anOpenCase(CaseSource.CUSTOMER_REPORT);
            decided.decide(Decision.release(), "Checked the photos", DECIDED);

            assertThatThrownBy(() -> decided.decide(Decision.fullRefund(), "Second thoughts", DECIDED))
                    .isInstanceOf(CaseAlreadyDecidedException.class);
            assertThatThrownBy(() -> decided.addEvidence(List.of(A_PHOTO))).isInstanceOf(CaseAlreadyDecidedException.class);
            assertThat(decided.decision()).contains(Decision.release());
        }

        @Test
        void aPartialRefundMustBeWithinTheProviderPrice() {
            Case opened = anOpenCase(CaseSource.CUSTOMER_REPORT);

            assertThatThrownBy(() -> opened.decide(Decision.partialRefund(rm("250.00")), "Checked the photos", DECIDED))
                    .isInstanceOf(InvalidDecisionAmountException.class);
            assertThatThrownBy(() -> opened.decide(Decision.partialRefund(rm("-0.01")), "Checked the photos", DECIDED))
                    .isInstanceOf(InvalidDecisionAmountException.class);
            assertThat(opened.isOpen()).isTrue();
        }

        @Test
        void aPartialRefundCanGoUpToTheProviderPrice() {
            Case opened = anOpenCase(CaseSource.CUSTOMER_REPORT);

            opened.decide(Decision.partialRefund(rm("200.00")), "Mostly done", DECIDED);

            assertThat(opened.isOpen()).isFalse();
        }

        @ParameterizedTest(name = "a case from {0} can be decided as {1}")
        @CsvSource({
            "CUSTOMER_REPORT,   release",
            "PROVIDER_REPORT,   full refund",
            "NO_SHOW_CHALLENGE, full refund",
            "MISSED_COMPLETION, partial refund"
        })
        void everySourceCanBeDecidedWithAnyOutcome(CaseSource source, String outcome) {
            Case opened = anOpenCase(source);

            opened.decide(decision(outcome), " Checked the photos ", DECIDED);

            assertThat(opened.status()).isEqualTo(CaseStatus.DECIDED);
            assertThat(opened.decision()).contains(decision(outcome));
            assertThat(opened.reason()).contains("Checked the photos");
            assertThat(opened.decidedAt()).contains(DECIDED);
        }

        @Test
        void theDecisionSaysWhatTheProviderReceives() {
            Case released = anOpenCase(CaseSource.CUSTOMER_REPORT);
            Case refunded = anOpenCase(CaseSource.CUSTOMER_REPORT);
            Case partial = anOpenCase(CaseSource.CUSTOMER_REPORT);
            assertThat(released.providerReceives()).isEmpty();

            released.decide(Decision.release(), "Work was done", DECIDED);
            refunded.decide(Decision.fullRefund(), "Work was not done", DECIDED);
            partial.decide(Decision.partialRefund(rm("120.00")), "Half done", DECIDED);

            assertThat(released.providerReceives()).contains(rm("200.00"));
            assertThat(refunded.providerReceives()).contains(rm("0.00"));
            assertThat(partial.providerReceives()).contains(rm("120.00"));
        }

        @Test
        void aDecisionNamesItsOutcome() {
            assertThat(Decision.release().outcome()).isEqualTo(Decision.Outcome.RELEASE);
            assertThat(Decision.fullRefund().providerReceives()).isEmpty();
            assertThat(Decision.partialRefund(rm("100")).outcome()).isEqualTo(Decision.Outcome.PARTIAL_REFUND);
        }
    }
}
