package catalogue.usecase;

import static catalogue.support.CatalogueWorld.ADMIN;
import static catalogue.support.CatalogueWorld.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.Retirement;
import catalogue.domain.exception.JobRetiredException;
import catalogue.domain.exception.RetirementReasonMissingException;
import catalogue.support.CatalogueWorld;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.MoveJobRequest;
import catalogue.usecase.dto.MoveJobResponse;
import catalogue.usecase.dto.RenameJobGroupRequest;
import catalogue.usecase.dto.RenameJobGroupResponse;
import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;
import catalogue.usecase.exception.JobGroupNotFoundException;
import catalogue.usecase.exception.JobNotFoundException;
import ie.events.JobAddedIE;
import ie.events.JobRetiredIE;
import org.junit.jupiter.api.Test;

class JobListUsecasesTest {

    private final CatalogueWorld world = new CatalogueWorld();
    private final String kitchen = world.group("Kitchen");

    @Test
    void anAddedGroupIsOnTheJobList() {
        assertThat(world.groups.find(kitchen)).get().satisfies(group -> assertThat(group.name()).isEqualTo("Kitchen"));
    }

    @Test
    void aGroupIsRenamed() {
        RenameJobGroupResponse response = new AdminRenameJobGroup(world.groups)
                .rename(new RenameJobGroupRequest(ADMIN, kitchen, "Kitchen and dining"));

        assertThat(response).isEqualTo(new RenameJobGroupResponse(kitchen, "Kitchen and dining"));
        assertThat(world.groups.find(kitchen).orElseThrow().name()).isEqualTo("Kitchen and dining");
        assertThat(world.groups.timesSaved(kitchen)).isEqualTo(2);
        assertThatThrownBy(() -> new AdminRenameJobGroup(world.groups).rename(new RenameJobGroupRequest(ADMIN, "none", "X")))
                .isInstanceOf(JobGroupNotFoundException.class);
    }

    @Test
    void anAddedJobIsSavedAndProvidersCanNowOfferIt() {
        String jobId = world.job("Kitchen care", kitchen);

        assertThat(world.jobs.find(jobId)).get().satisfies(job -> {
            assertThat(job.name()).isEqualTo("Kitchen care");
            assertThat(job.groupId()).isEqualTo(kitchen);
        });
        assertThat(world.publisher.published()).containsExactly(new JobAddedIE(jobId, "Kitchen care", kitchen, NOW));
    }

    @Test
    void aJobIsOnlyAddedToAGroupOnTheList() {
        AdminAddJob usecase = new AdminAddJob(world.jobs, world.groups, world.publisher, world.clock);

        assertThatThrownBy(() -> usecase.add(new AddJobRequest(ADMIN, "Kitchen care", "no-group")))
                .isInstanceOf(JobGroupNotFoundException.class);
        assertThat(world.jobs.all()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    void aJobIsRenamed() {
        String jobId = world.job("Kitchen care", kitchen);

        RenameJobResponse response = new AdminRenameJob(world.jobs).rename(new RenameJobRequest(ADMIN, jobId, "Kitchen deep clean"));

        assertThat(response).isEqualTo(new RenameJobResponse(jobId, "Kitchen deep clean"));
        assertThat(world.jobs.find(jobId).orElseThrow().name()).isEqualTo("Kitchen deep clean");
        assertThat(world.jobs.timesSaved(jobId)).isEqualTo(2);
        assertThatThrownBy(() -> new AdminRenameJob(world.jobs).rename(new RenameJobRequest(ADMIN, "none", "X")))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void aJobIsMovedToAnotherGroupOnTheList() {
        String jobId = world.job("Tidying", kitchen);
        String livingRoom = world.group("Living room");
        AdminMoveJob usecase = new AdminMoveJob(world.jobs, world.groups);

        MoveJobResponse response = usecase.move(new MoveJobRequest(ADMIN, jobId, livingRoom));

        assertThat(response).isEqualTo(new MoveJobResponse(jobId, livingRoom));
        assertThat(world.jobs.find(jobId).orElseThrow().groupId()).isEqualTo(livingRoom);
        assertThat(world.jobs.timesSaved(jobId)).isEqualTo(2);
        assertThatThrownBy(() -> usecase.move(new MoveJobRequest(ADMIN, jobId, "none")))
                .isInstanceOf(JobGroupNotFoundException.class);
        assertThatThrownBy(() -> usecase.move(new MoveJobRequest(ADMIN, "none", livingRoom)))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void aRetiredJobIsKeptAndTheOtherContextsAreTold() {
        String jobId = world.job("Kitchen care", kitchen);
        AdminRetireJob usecase = new AdminRetireJob(world.jobs, world.showcases, world.publisher, world.clock);

        RetireJobResponse response = usecase.retire(new RetireJobRequest(ADMIN, jobId, "No demand"));

        assertThat(response).isEqualTo(new RetireJobResponse(jobId, NOW));
        assertThat(world.jobs.find(jobId).orElseThrow().retirement()).contains(new Retirement("No demand", NOW));
        assertThat(world.jobs.timesSaved(jobId)).isEqualTo(2);
        assertThat(world.publisher.published(JobRetiredIE.class)).containsExactly(new JobRetiredIE(jobId, "No demand", NOW));
    }

    @Test
    void aRetiredJobComesOffTheFeaturedSearches() {
        String kitchenCare = world.job("Kitchen care", kitchen);
        String laundry = world.job("Laundry folding", kitchen);
        world.featured("Weekend quick refresh", kitchenCare, laundry);
        world.featured("Laundry day", laundry);

        new AdminRetireJob(world.jobs, world.showcases, world.publisher, world.clock)
                .retire(new RetireJobRequest(ADMIN, laundry, "No demand"));

        assertThat(world.showcases.get().shown()).singleElement()
                .satisfies(search -> assertThat(search.jobIds()).containsExactly(kitchenCare));
    }

    @Test
    void retiringWithoutAReasonOrTwiceChangesNothing() {
        String jobId = world.job("Kitchen care", kitchen);
        AdminRetireJob usecase = new AdminRetireJob(world.jobs, world.showcases, world.publisher, world.clock);

        assertThatThrownBy(() -> usecase.retire(new RetireJobRequest(ADMIN, jobId, " ")))
                .isInstanceOf(RetirementReasonMissingException.class);
        assertThat(world.publisher.published(JobRetiredIE.class)).isEmpty();
        assertThat(world.showcases.saves()).isZero();

        usecase.retire(new RetireJobRequest(ADMIN, jobId, "No demand"));
        assertThatThrownBy(() -> usecase.retire(new RetireJobRequest(ADMIN, jobId, "Again")))
                .isInstanceOf(JobRetiredException.class);
        assertThatThrownBy(() -> usecase.retire(new RetireJobRequest(ADMIN, "none", "No demand")))
                .isInstanceOf(JobNotFoundException.class);
        assertThat(world.publisher.published(JobRetiredIE.class)).hasSize(1);
    }
}
