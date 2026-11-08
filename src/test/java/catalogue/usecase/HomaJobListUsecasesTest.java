package catalogue.usecase;

import static catalogue.usecase.CatalogueFakes.CLOCK;
import static catalogue.usecase.CatalogueFakes.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.JobCode;
import catalogue.domain.JobGroup;
import catalogue.domain.JobList;
import catalogue.domain.exception.DuplicateJobCodeException;
import catalogue.domain.exception.JobAlreadyRetiredException;
import catalogue.domain.exception.JobNotFoundException;
import catalogue.usecase.CatalogueFakes.InMemoryJobLists;
import catalogue.usecase.CatalogueFakes.RecordingPublisher;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddJobResponse;
import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;
import ie.events.JobAddedIE;
import ie.events.JobRetiredIE;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Homa curates its job list: adding, renaming and retiring jobs. */
class HomaJobListUsecasesTest {

    private final InMemoryJobLists jobLists = new InMemoryJobLists();
    private final RecordingPublisher events = new RecordingPublisher();

    private InMemoryJobLists aJobListWith(String... codes) {
        JobList list = JobList.create();
        for (String code : codes) {
            list.add(JobCode.of(code), "Job " + code, JobGroup.of("Kitchen"));
        }
        return jobLists.holding(list);
    }

    @Nested
    class HomaAddsAJob {

        private final HomaAddJob addJob = new HomaAddJob(jobLists, events, CLOCK);

        @Test
        void theFirstJobStartsTheJobListAndProvidersAreToldItCanBeOffered() {
            AddJobResponse response = addJob.add(new AddJobRequest("KITCHEN_GREASE", " Degrease the kitchen ", "Kitchen"));

            assertThat(response).isEqualTo(new AddJobResponse("KITCHEN_GREASE", "Degrease the kitchen", "Kitchen"));
            assertThat(jobLists.saves).isEqualTo(1);
            assertThat(jobLists.stored.isOffered(JobCode.of("KITCHEN_GREASE"))).isTrue();
            assertThat(events.published).containsExactly(new JobAddedIE("KITCHEN_GREASE", "Degrease the kitchen", "Kitchen", NOW));
        }

        @Test
        void aJobIsAddedToTheExistingJobList() {
            JobList existing = aJobListWith("TOILET_CARE").stored;

            addJob.add(new AddJobRequest("KITCHEN_GREASE", "Degrease the kitchen", "Kitchen"));

            assertThat(jobLists.stored).isSameAs(existing);
            assertThat(existing.offeredCodes()).containsExactlyInAnyOrder(JobCode.of("TOILET_CARE"), JobCode.of("KITCHEN_GREASE"));
        }

        @Test
        void aRejectedJobIsNeitherSavedNorAnnounced() {
            aJobListWith("KITCHEN_GREASE");

            assertThatThrownBy(() -> addJob.add(new AddJobRequest("KITCHEN_GREASE", "Again", "Kitchen")))
                    .isInstanceOf(DuplicateJobCodeException.class);
            assertThat(jobLists.saves).isZero();
            assertThat(events.published).isEmpty();
        }
    }

    @Nested
    class HomaRenamesAJob {

        private final HomaRenameJob renameJob = new HomaRenameJob(jobLists);

        @Test
        void aRenamedJobIsSavedWithItsNewName() {
            aJobListWith("KITCHEN_GREASE");

            RenameJobResponse response = renameJob.rename(new RenameJobRequest("KITCHEN_GREASE", " Kitchen degreasing "));

            assertThat(response).isEqualTo(new RenameJobResponse("KITCHEN_GREASE", "Kitchen degreasing"));
            assertThat(jobLists.saves).isEqualTo(1);
            assertThat(jobLists.stored.job(JobCode.of("KITCHEN_GREASE")).orElseThrow().name()).isEqualTo("Kitchen degreasing");
        }

        @Test
        void onlyAJobOnTheListCanBeRenamed() {
            assertThatThrownBy(() -> renameJob.rename(new RenameJobRequest("KITCHEN_GREASE", "Kitchen degreasing")))
                    .isInstanceOf(JobNotFoundException.class);
            assertThat(jobLists.saves).isZero();
        }
    }

    @Nested
    class HomaRetiresAJob {

        private final HomaRetireJob retireJob = new HomaRetireJob(jobLists, events, CLOCK);

        @Test
        void aRetiredJobIsSavedAndProvidersAreToldItCanNoLongerBeOffered() {
            aJobListWith("WARDROBE_ORGANISE");

            RetireJobResponse response = retireJob.retire(new RetireJobRequest("WARDROBE_ORGANISE"));

            assertThat(response).isEqualTo(new RetireJobResponse("WARDROBE_ORGANISE", NOW));
            assertThat(jobLists.saves).isEqualTo(1);
            assertThat(jobLists.stored.isOffered(JobCode.of("WARDROBE_ORGANISE"))).isFalse();
            assertThat(events.published).containsExactly(new JobRetiredIE("WARDROBE_ORGANISE", NOW));
        }

        @Test
        void aJobRetiredTwiceIsNeitherSavedNorAnnouncedAgain() {
            aJobListWith("WARDROBE_ORGANISE").stored.retire(JobCode.of("WARDROBE_ORGANISE"));

            assertThatThrownBy(() -> retireJob.retire(new RetireJobRequest("WARDROBE_ORGANISE")))
                    .isInstanceOf(JobAlreadyRetiredException.class);
            assertThat(jobLists.saves).isZero();
            assertThat(events.published).isEmpty();
        }

        @Test
        void onlyAJobOnTheListCanBeRetired() {
            assertThatThrownBy(() -> retireJob.retire(new RetireJobRequest("WARDROBE_ORGANISE")))
                    .isInstanceOf(JobNotFoundException.class);
        }
    }
}
