package catalogue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.JobRetiredException;
import catalogue.domain.exception.NameMissingException;
import catalogue.domain.exception.RetirementReasonMissingException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobTest {

    private static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");
    private final JobGroup kitchen = JobGroup.add("Kitchen");
    private final JobGroup livingRoom = JobGroup.add("Living room");

    @Test
    void aNewJobJoinsItsGroupWithItsOwnCode() {
        Job job = Job.add(" Kitchen care ", kitchen);

        assertThat(job.id()).isNotBlank().isNotEqualTo(Job.add("Kitchen care", kitchen).id());
        assertThat(job.name()).isEqualTo("Kitchen care");
        assertThat(job.groupId()).isEqualTo(kitchen.id());
        assertThat(job.isRetired()).isFalse();
        assertThat(job.retirement()).isEmpty();
    }

    @Test
    void aJobNeedsAName() {
        assertThatThrownBy(() -> Job.add(" ", kitchen)).isInstanceOf(NameMissingException.class);
        assertThatThrownBy(() -> Job.add(null, kitchen)).isInstanceOf(NameMissingException.class);
    }

    @Test
    void renamingAJobKeepsItsCode() {
        Job job = Job.add("Kitchen care", kitchen);
        String code = job.id();

        job.rename("Kitchen deep clean");

        assertThat(job.name()).isEqualTo("Kitchen deep clean");
        assertThat(job.id()).isEqualTo(code);
        assertThatThrownBy(() -> job.rename("")).isInstanceOf(NameMissingException.class);
    }

    @Test
    void aJobCanMoveToAnotherGroup() {
        Job job = Job.add("Tidying", kitchen);

        job.moveTo(livingRoom);

        assertThat(job.groupId()).isEqualTo(livingRoom.id());
    }

    @Test
    void retiringAJobKeepsItWithTheReasonAndTime() {
        Job job = Job.add("Kitchen care", kitchen);

        job.retire("Replaced by two smaller jobs", NOW);

        assertThat(job.isRetired()).isTrue();
        assertThat(job.retirement()).contains(new Retirement("Replaced by two smaller jobs", NOW));
        assertThat(job.name()).isEqualTo("Kitchen care");
    }

    @Test
    void retiringAJobNeedsAReason() {
        Job job = Job.add("Kitchen care", kitchen);

        assertThatThrownBy(() -> job.retire(" ", NOW)).isInstanceOf(RetirementReasonMissingException.class);
        assertThatThrownBy(() -> job.retire(null, NOW)).isInstanceOf(RetirementReasonMissingException.class);
        assertThat(job.isRetired()).isFalse();
    }

    @Test
    void aRetiredJobCanNoLongerBeOfferedOrChanged() {
        Job job = Job.add("Kitchen care", kitchen);
        job.requireOfferable();
        job.retire("No demand", NOW);

        assertThatThrownBy(job::requireOfferable).isInstanceOf(JobRetiredException.class);
        assertThatThrownBy(() -> job.rename("Other")).isInstanceOf(JobRetiredException.class);
        assertThatThrownBy(() -> job.moveTo(livingRoom)).isInstanceOf(JobRetiredException.class);
        assertThatThrownBy(() -> job.retire("Again", NOW)).isInstanceOf(JobRetiredException.class);
        assertThat(job.name()).isEqualTo("Kitchen care");
        assertThat(job.groupId()).isEqualTo(kitchen.id());
        assertThat(job.retirement()).contains(new Retirement("No demand", NOW));
    }

    @Test
    void aGroupIsNamedAndCanBeRenamed() {
        JobGroup group = JobGroup.add(" Kitchen ");
        assertThat(group.name()).isEqualTo("Kitchen");
        assertThat(group.id()).isNotBlank().isNotEqualTo(kitchen.id());

        group.rename("Kitchen and dining");

        assertThat(group.name()).isEqualTo("Kitchen and dining");
        assertThatThrownBy(() -> group.rename(" ")).isInstanceOf(NameMissingException.class);
        assertThatThrownBy(() -> JobGroup.add("")).isInstanceOf(NameMissingException.class);
    }
}
