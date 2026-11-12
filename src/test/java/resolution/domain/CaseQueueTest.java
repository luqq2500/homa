package resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import kernel.Money;
import org.junit.jupiter.api.Test;
import resolution.domain.exception.CaseAlreadyDecidedException;
import resolution.domain.exception.CaseNotOpenException;

/** Feature: Safety, damage or loss reports first in the queue, and a case taken over by a flag. */
class CaseQueueTest {

    private static final Instant FRIDAY = Instant.parse("2026-11-13T06:00:00Z");
    private static final Instant SATURDAY = Instant.parse("2026-11-14T06:00:00Z");
    private static final Instant SUNDAY = Instant.parse("2026-11-15T06:00:00Z");
    private static final EvidenceRef A_PHOTO = EvidenceRef.of("evidence/vase.jpg");

    private static Case aCaseOpenedOn(String reservationId, Instant openedAt) {
        return Case.open(reservationId, CaseSource.CUSTOMER_REPORT, List.of(A_PHOTO), Money.parse("200.00"), openedAt);
    }

    private static Case aSafetyCaseOpenedOn(String reservationId, Instant openedAt) {
        Case safety = aCaseOpenedOn(reservationId, openedAt);
        safety.markSafety();
        return safety;
    }

    @Test
    void aNewCaseIsNotAboutSafety() {
        assertThat(aCaseOpenedOn("reservation-1", FRIDAY).isSafety()).isFalse();
    }

    @Test
    void safetyCasesComeFirstInTheQueue() {
        Case older = aCaseOpenedOn("reservation-1", FRIDAY);
        Case safety = aSafetyCaseOpenedOn("reservation-2", SATURDAY);

        assertThat(List.of(older, safety).stream().sorted(Case.QUEUE_ORDER).toList()).containsExactly(safety, older);
        assertThat(List.of(safety, older).stream().sorted(Case.QUEUE_ORDER).toList()).containsExactly(safety, older);
    }

    @Test
    void theRestAreOldestFirst() {
        Case friday = aCaseOpenedOn("reservation-1", FRIDAY);
        Case sunday = aCaseOpenedOn("reservation-2", SUNDAY);
        Case safetySaturday = aSafetyCaseOpenedOn("reservation-3", SATURDAY);
        Case safetySunday = aSafetyCaseOpenedOn("reservation-4", SUNDAY);

        assertThat(List.of(sunday, safetySunday, friday, safetySaturday).stream().sorted(Case.QUEUE_ORDER).toList())
                .containsExactly(safetySaturday, safetySunday, friday, sunday);
    }

    @Test
    void aSafetyReportJoiningACaseMarksIt() {
        Case finishedWork = aCaseOpenedOn("reservation-1", FRIDAY);

        finishedWork.addEvidence(List.of(A_PHOTO));
        finishedWork.markSafety();

        assertThat(finishedWork.isSafety()).isTrue();
    }

    @Test
    void aDecidedCaseIsNotMarked() {
        Case decided = aCaseOpenedOn("reservation-1", FRIDAY);
        decided.decide(Decision.release(), "Checked the photos", SATURDAY);

        assertThatThrownBy(decided::markSafety).isInstanceOf(CaseAlreadyDecidedException.class);
        assertThat(decided.isSafety()).isFalse();
    }

    @Test
    void aCaseTakenOverByAFlagIsClosed() {
        Case taken = aCaseOpenedOn("reservation-1", FRIDAY);

        taken.closeForProviderFlag(SATURDAY);

        assertThat(taken.status()).isEqualTo(CaseStatus.CLOSED);
        assertThat(taken.isOpen()).isFalse();
        assertThat(taken.closedAt()).contains(SATURDAY);
        assertThat(taken.decision()).isEmpty();
        assertThat(taken.providerReceives()).isEmpty();
        assertThatThrownBy(() -> taken.decide(Decision.release(), "Checked the photos", SUNDAY))
                .isInstanceOf(CaseNotOpenException.class)
                .hasMessageContaining(taken.id());
        assertThatThrownBy(() -> taken.addEvidence(List.of(A_PHOTO))).isInstanceOf(CaseNotOpenException.class);
        assertThatThrownBy(() -> taken.closeForProviderFlag(SUNDAY)).isInstanceOf(CaseNotOpenException.class);
    }

    @Test
    void anOpenCaseHasNotClosed() {
        Case open = aCaseOpenedOn("reservation-1", FRIDAY);

        assertThat(open.closedAt()).isEmpty();
        assertThatThrownBy(() -> open.closeForProviderFlag(null)).isInstanceOf(NullPointerException.class);
        assertThat(open.isOpen()).isTrue();
    }

    @Test
    void aDecidedCaseIsNotClosedByAFlag() {
        Case decided = aCaseOpenedOn("reservation-1", FRIDAY);
        decided.decide(Decision.release(), "Checked the photos", SATURDAY);

        assertThatThrownBy(() -> decided.closeForProviderFlag(SUNDAY)).isInstanceOf(CaseAlreadyDecidedException.class);
        assertThat(decided.status()).isEqualTo(CaseStatus.DECIDED);
    }
}
