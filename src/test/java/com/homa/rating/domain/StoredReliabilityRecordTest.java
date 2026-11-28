package com.homa.rating.domain;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.rating.domain.RatingFixtures.AISYAH_PARTY;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_PARTY;
import static com.homa.rating.domain.RatingFixtures.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredReliabilityRecordTest {

    private static final Instant WHEN = at(2026, 12, 4, 22, 0);

    @Test
    @Tag("RTG-10")
    @DisplayName("A stored record comes back with its incidents and the version it was saved at, and keeps counting.")
    void aStoredRecordComesBack() {
        Incident cancellation = Incident.of(IncidentKind.CANCELLATION, RESERVATION, WHEN);
        Incident left = Incident.memberLeft(RESERVATION, "member-nurul", WHEN);

        ReliabilityRecord record = ReliabilityRecord.restore(KEMAS_HOME_PARTY, List.of(cancellation, left), 4);

        assertThat(record.party()).isEqualTo(KEMAS_HOME_PARTY);
        assertThat(record.incidents()).containsExactly(cancellation, left);
        assertThat(record.version()).isEqualTo(4);
        assertThat(record.countsAt(WHEN)).isEqualTo(new ReliabilityCounts(1, 0, 0, 1));
        assertThat(record.recordMemberLeft(RESERVATION, "member-nurul", WHEN)).isFalse();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A stored incident older than 12 months comes back too: the purge removes it later, and the count "
            + "already leaves it out.")
    void aStoredOldIncidentIsNotRefused() {
        Incident old = Incident.of(IncidentKind.NO_SHOW, RESERVATION, at(2025, 1, 1, 10, 0));

        ReliabilityRecord record = ReliabilityRecord.restore(KEMAS_HOME_PARTY, List.of(old), 1);

        assertThat(record.incidents()).containsExactly(old);
        assertThat(record.countsAt(WHEN).noShows()).isZero();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A stored customer record can't hold a no-roster or a member who left.")
    void aStoredCustomerRecordHoldsOnlyWhatACustomerCanHave() {
        assertThatThrownBy(() -> ReliabilityRecord.restore(AISYAH_PARTY,
                List.of(Incident.of(IncidentKind.NO_ROSTER, RESERVATION, WHEN)), 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("NO_ROSTER");
        assertThat(ReliabilityRecord.restore(AISYAH_PARTY,
                List.of(Incident.of(IncidentKind.NO_SHOW, RESERVATION, WHEN)), 0).incidents()).hasSize(1);
    }
}
