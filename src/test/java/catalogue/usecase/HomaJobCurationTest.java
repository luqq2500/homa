package catalogue.usecase;

import static catalogue.usecase.CatalogueUsecaseFixtures.CLOCK;
import static catalogue.usecase.CatalogueUsecaseFixtures.FOUNDER;
import static catalogue.usecase.CatalogueUsecaseFixtures.NOW;
import static catalogue.usecase.CatalogueUsecaseFixtures.recorded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.JobCode;
import catalogue.domain.exception.DuplicateJobCodeException;
import catalogue.domain.exception.JobAlreadyRetiredException;
import catalogue.domain.exception.JobNotFoundException;
import catalogue.usecase.CatalogueUsecaseFixtures.AuditLog;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryHomepages;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryJobLists;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryShelves;
import catalogue.usecase.CatalogueUsecaseFixtures.PublishedEvents;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddJobResponse;
import catalogue.usecase.dto.CreateShelfRequest;
import catalogue.usecase.dto.RegroupJobRequest;
import catalogue.usecase.dto.RegroupJobResponse;
import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;
import catalogue.usecase.exception.AdminReasonRequiredException;
import catalogue.usecase.exception.ChangedMeanwhileException;
import catalogue.usecase.exception.DecidedByRequiredException;
import catalogue.usecase.port.out.AdminAction;
import catalogue.domain.Shelf;
import ie.events.JobAddedIE;
import ie.events.JobRegroupedIE;
import ie.events.JobRenamedIE;
import ie.events.JobRetiredIE;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Homa's founders curate the job list: they add, rename and retire jobs. */
class HomaJobCurationTest {

    private final InMemoryJobLists jobLists = new InMemoryJobLists();
    private final PublishedEvents events = new PublishedEvents();
    private final AuditLog auditLog = new AuditLog();
    private final HomaAddJob addJob = new HomaAddJob(jobLists, events, auditLog, CLOCK);
    private final InMemoryShelves shelves = new InMemoryShelves();
    private final InMemoryHomepages homepages = new InMemoryHomepages();
    private final HomaRenameJob renameJob = new HomaRenameJob(jobLists, events, auditLog, CLOCK);
    private final HomaRegroupJob regroupJob = new HomaRegroupJob(jobLists, events, auditLog, CLOCK);
    private final HomaRetireJob retireJob = new HomaRetireJob(jobLists, shelves, events, auditLog, CLOCK);

    private void kitchenCareIsOnTheList() {
        addJob.add(new AddJobRequest(FOUNDER, "KITCHEN", "Kitchen care", "Kitchen"));
    }

    private boolean isOffered(String code) {
        return jobLists.find().orElseThrow().isOffered(JobCode.of(code));
    }

    @Nested
    class WhenAnotherFounderSavedFirst {

        @Test
        void anAddIsRefusedAndNothingIsToldOrRecorded() {
            jobLists.anotherFounderSavesFirst();

            assertThatThrownBy(() -> addJob.add(new AddJobRequest(FOUNDER, "KITCHEN", "Kitchen care", "Kitchen")))
                    .isInstanceOf(ChangedMeanwhileException.class)
                    .hasMessage("The job list changed meanwhile; try again on what is there now");

            assertThat(jobLists.find()).isEmpty();
            assertThat(events.all).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aRenameIsRefusedAndNothingIsToldOrRecorded() {
            kitchenCareIsOnTheList();
            jobLists.anotherFounderSavesFirst();

            assertThatThrownBy(() -> renameJob.rename(new RenameJobRequest(FOUNDER, "KITCHEN", "Kitchen refresh")))
                    .isInstanceOf(ChangedMeanwhileException.class);

            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @Test
        void aRegroupIsRefusedAndNothingIsToldOrRecorded() {
            kitchenCareIsOnTheList();
            jobLists.anotherFounderSavesFirst();

            assertThatThrownBy(() -> regroupJob.regroup(new RegroupJobRequest(FOUNDER, "KITCHEN", "Living room")))
                    .isInstanceOf(ChangedMeanwhileException.class);

            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @Test
        void aRetirementIsRefusedAndNothingIsToldOrRecorded() {
            kitchenCareIsOnTheList();
            jobLists.anotherFounderSavesFirst();

            assertThatThrownBy(() -> retireJob.retire(new RetireJobRequest(FOUNDER, "KITCHEN", "Replaced")))
                    .isInstanceOf(ChangedMeanwhileException.class);

            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }
    }

    @Nested
    class Adding {

        @Test
        void anAddedJobIsSavedAndProvidersCanOfferIt() {
            AddJobResponse added = addJob.add(new AddJobRequest(FOUNDER, "KITCHEN", "Kitchen care", "Kitchen"));

            assertThat(added).isEqualTo(new AddJobResponse("KITCHEN", "Kitchen care", "Kitchen"));
            assertThat(added.code()).isEqualTo("KITCHEN");
            assertThat(added.name()).isEqualTo("Kitchen care");
            assertThat(added.group()).isEqualTo("Kitchen");
            assertThat(jobLists.saves).isEqualTo(1);
            assertThat(isOffered("KITCHEN")).isTrue();
        }

        @Test
        void anAddedJobIsToldToTheOtherContexts() {
            kitchenCareIsOnTheList();

            assertThat(events.all).containsExactly(new JobAddedIE("KITCHEN", "Kitchen care", "Kitchen", NOW));
        }

        @Test
        void anAddedJobIsRecordedWithWhoAddedItAndWhen() {
            kitchenCareIsOnTheList();

            assertThat(auditLog.all).containsExactly(recorded("ADD_JOB", "KITCHEN"));
            AdminAction action = auditLog.all.get(0);
            assertThat(action.decidedBy()).isEqualTo(FOUNDER);
            assertThat(action.action()).isEqualTo("ADD_JOB");
            assertThat(action.subject()).isEqualTo("KITCHEN");
            assertThat(action.reason()).isEmpty();
            assertThat(action.at()).isEqualTo(NOW);
        }

        @Test
        void aSecondJobJoinsTheSameJobList() {
            kitchenCareIsOnTheList();
            String listId = jobLists.find().orElseThrow().id();

            addJob.add(new AddJobRequest(FOUNDER, "LAUNDRY_FOLD", "Laundry folding", "Laundry"));

            assertThat(jobLists.find().orElseThrow().id()).isEqualTo(listId);
            assertThat(isOffered("KITCHEN")).isTrue();
            assertThat(isOffered("LAUNDRY_FOLD")).isTrue();
            assertThat(jobLists.saves).isEqualTo(2);
        }

        @Test
        void aJobCodeAlreadyOnTheListIsRefusedAndNothingIsSavedToldOrRecorded() {
            kitchenCareIsOnTheList();

            assertThatThrownBy(() -> addJob.add(new AddJobRequest(FOUNDER, "KITCHEN", "Kitchen deep clean", "Kitchen")))
                    .isInstanceOf(DuplicateJobCodeException.class);

            assertThat(jobLists.saves).isEqualTo(1);
            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @ParameterizedTest(name = "decided by \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   "})
        void addingAJobNeedsTheFounderWhoDecidedIt(String decidedBy) {
            assertThatThrownBy(() -> addJob.add(new AddJobRequest(decidedBy, "KITCHEN", "Kitchen care", "Kitchen")))
                    .isInstanceOf(DecidedByRequiredException.class)
                    .hasMessage("An admin action needs the founder who decided it");

            assertThat(jobLists.find()).isEmpty();
            assertThat(events.all).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void theFoundersIdIsRecordedWithoutSpacesAroundIt() {
            addJob.add(new AddJobRequest("  " + FOUNDER + " ", "KITCHEN", "Kitchen care", "Kitchen"));

            assertThat(auditLog.all).containsExactly(recorded("ADD_JOB", "KITCHEN"));
        }
    }

    @Nested
    class Renaming {

        @Test
        void aRenamedJobIsSavedWithItsNewName() {
            kitchenCareIsOnTheList();

            RenameJobResponse renamed = renameJob.rename(new RenameJobRequest(FOUNDER, "KITCHEN", "Kitchen refresh"));

            assertThat(renamed).isEqualTo(new RenameJobResponse("KITCHEN", "Kitchen refresh"));
            assertThat(renamed.code()).isEqualTo("KITCHEN");
            assertThat(renamed.name()).isEqualTo("Kitchen refresh");
            assertThat(jobLists.saves).isEqualTo(2);
            assertThat(jobLists.find().orElseThrow().job(JobCode.of("KITCHEN")).orElseThrow().name()).isEqualTo("Kitchen refresh");
        }

        @Test
        void aRenameIsToldToTheOtherContextsSoTheNameTheyShowIsUpdated() {
            kitchenCareIsOnTheList();

            renameJob.rename(new RenameJobRequest(FOUNDER, "KITCHEN", "Kitchen refresh"));

            assertThat(events.all).hasSize(2).last().isEqualTo(new JobRenamedIE("KITCHEN", "Kitchen refresh", NOW));
            assertThat(auditLog.all).containsExactly(recorded("ADD_JOB", "KITCHEN"), recorded("RENAME_JOB", "KITCHEN"));
        }

        @Test
        void renamingAJobToItsOwnNameSavesTellsAndRecordsNothing() {
            kitchenCareIsOnTheList();

            RenameJobResponse renamed = renameJob.rename(new RenameJobRequest(FOUNDER, "KITCHEN", " Kitchen care "));

            assertThat(renamed).isEqualTo(new RenameJobResponse("KITCHEN", "Kitchen care"));
            assertThat(jobLists.saves).isEqualTo(1);
            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @Test
        void aJobThatIsNotOnTheListIsNotRenamed() {
            assertThatThrownBy(() -> renameJob.rename(new RenameJobRequest(FOUNDER, "KITCHEN", "Kitchen refresh")))
                    .isInstanceOf(JobNotFoundException.class);

            assertThat(jobLists.saves).isZero();
            assertThat(events.all).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void renamingAJobNeedsTheFounderWhoDecidedIt() {
            kitchenCareIsOnTheList();

            assertThatThrownBy(() -> renameJob.rename(new RenameJobRequest(" ", "KITCHEN", "Kitchen refresh")))
                    .isInstanceOf(DecidedByRequiredException.class);

            assertThat(jobLists.saves).isEqualTo(1);
            assertThat(jobLists.find().orElseThrow().job(JobCode.of("KITCHEN")).orElseThrow().name()).isEqualTo("Kitchen care");
        }
    }

    @Nested
    class Regrouping {

        @Test
        void aRegroupedJobIsSavedInItsNewGroupWithTheSameCodeAndName() {
            kitchenCareIsOnTheList();

            RegroupJobResponse moved = regroupJob.regroup(new RegroupJobRequest(FOUNDER, "KITCHEN", "Living room"));

            assertThat(moved).isEqualTo(new RegroupJobResponse("KITCHEN", "Living room"));
            assertThat(moved.code()).isEqualTo("KITCHEN");
            assertThat(moved.group()).isEqualTo("Living room");
            assertThat(jobLists.saves).isEqualTo(2);
            assertThat(jobLists.find().orElseThrow().job(JobCode.of("KITCHEN")).orElseThrow().group().name()).isEqualTo("Living room");
            assertThat(isOffered("KITCHEN")).isTrue();
        }

        @Test
        void aRegroupIsToldToTheOtherContextsAndRecorded() {
            kitchenCareIsOnTheList();

            regroupJob.regroup(new RegroupJobRequest(FOUNDER, "KITCHEN", "Living room"));

            assertThat(events.all).hasSize(2).last().isEqualTo(new JobRegroupedIE("KITCHEN", "Living room", NOW));
            assertThat(auditLog.all).last().isEqualTo(recorded("REGROUP_JOB", "KITCHEN"));
        }

        @Test
        void movingAJobToItsOwnGroupSavesTellsAndRecordsNothing() {
            kitchenCareIsOnTheList();

            RegroupJobResponse moved = regroupJob.regroup(new RegroupJobRequest(FOUNDER, "KITCHEN", "Kitchen"));

            assertThat(moved).isEqualTo(new RegroupJobResponse("KITCHEN", "Kitchen"));
            assertThat(jobLists.saves).isEqualTo(1);
            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @Test
        void aJobThatIsNotOnTheListIsNotRegrouped() {
            assertThatThrownBy(() -> regroupJob.regroup(new RegroupJobRequest(FOUNDER, "KITCHEN", "Living room")))
                    .isInstanceOf(JobNotFoundException.class);

            assertThat(jobLists.saves).isZero();
            assertThat(events.all).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void regroupingAJobNeedsTheFounderWhoDecidedIt() {
            kitchenCareIsOnTheList();

            assertThatThrownBy(() -> regroupJob.regroup(new RegroupJobRequest("", "KITCHEN", "Living room")))
                    .isInstanceOf(DecidedByRequiredException.class);

            assertThat(jobLists.find().orElseThrow().job(JobCode.of("KITCHEN")).orElseThrow().group().name()).isEqualTo("Kitchen");
            assertThat(events.all).hasSize(1);
        }
    }

    @Nested
    class Retiring {

        @Test
        void aRetiredJobIsSavedAndProvidersCanNoLongerOfferIt() {
            kitchenCareIsOnTheList();

            RetireJobResponse retired = retireJob.retire(new RetireJobRequest(FOUNDER, "KITCHEN", "Replaced by two smaller jobs"));

            assertThat(retired).isEqualTo(new RetireJobResponse("KITCHEN", NOW));
            assertThat(retired.code()).isEqualTo("KITCHEN");
            assertThat(retired.retiredAt()).isEqualTo(NOW);
            assertThat(jobLists.saves).isEqualTo(2);
            assertThat(isOffered("KITCHEN")).isFalse();
        }

        @Test
        void aRetiredJobIsToldToTheOtherContextsSoItComesOffTheListings() {
            kitchenCareIsOnTheList();

            retireJob.retire(new RetireJobRequest(FOUNDER, "KITCHEN", "Replaced by two smaller jobs"));

            assertThat(events.all).last().isEqualTo(new JobRetiredIE("KITCHEN", NOW));
            assertThat(events.all).hasSize(2);
        }

        @Test
        void aRetirementIsRecordedWithItsReason() {
            kitchenCareIsOnTheList();

            retireJob.retire(new RetireJobRequest(FOUNDER, "KITCHEN", "  Replaced by two smaller jobs "));

            assertThat(auditLog.all).last()
                    .isEqualTo(new AdminAction(FOUNDER, "RETIRE_JOB", "KITCHEN", "Replaced by two smaller jobs", NOW));
        }

        @ParameterizedTest(name = "reason \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   "})
        void retiringAJobNeedsAReason(String reason) {
            kitchenCareIsOnTheList();

            assertThatThrownBy(() -> retireJob.retire(new RetireJobRequest(FOUNDER, "KITCHEN", reason)))
                    .isInstanceOf(AdminReasonRequiredException.class)
                    .hasMessage("A reason is required to retire a job");

            assertThat(isOffered("KITCHEN")).isTrue();
            assertThat(jobLists.saves).isEqualTo(1);
            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @Test
        void retiringAJobNeedsTheFounderWhoDecidedIt() {
            kitchenCareIsOnTheList();

            assertThatThrownBy(() -> retireJob.retire(new RetireJobRequest(null, "KITCHEN", "Replaced")))
                    .isInstanceOf(DecidedByRequiredException.class);

            assertThat(isOffered("KITCHEN")).isTrue();
        }

        @Test
        void aJobIsRetiredOnlyOnce() {
            kitchenCareIsOnTheList();
            retireJob.retire(new RetireJobRequest(FOUNDER, "KITCHEN", "Replaced by two smaller jobs"));

            assertThatThrownBy(() -> retireJob.retire(new RetireJobRequest(FOUNDER, "KITCHEN", "Again")))
                    .isInstanceOf(JobAlreadyRetiredException.class);

            assertThat(jobLists.saves).isEqualTo(2);
            assertThat(events.all).hasSize(2);
            assertThat(auditLog.all).hasSize(2);
        }

        @Test
        void aRetiredJobComesOffTheShelvesThatSearchForIt() {
            kitchenCareIsOnTheList();
            addJob.add(new AddJobRequest(FOUNDER, "LAUNDRY_FOLD", "Laundry folding", "Laundry"));
            HomaCreateShelf createShelf = new HomaCreateShelf(shelves, homepages, jobLists, auditLog, CLOCK);
            createShelf.create(new CreateShelfRequest(FOUNDER, "Weekend quick refresh", Set.of("KITCHEN", "LAUNDRY_FOLD"), 2, 1, 1));
            createShelf.create(new CreateShelfRequest(FOUNDER, "Kitchen rescue", Set.of("KITCHEN"), 2, 1, 2));
            createShelf.create(new CreateShelfRequest(FOUNDER, "Laundry day", Set.of("LAUNDRY_FOLD"), 2, 1, 3));
            int savesBefore = shelves.saves;

            retireJob.retire(new RetireJobRequest(FOUNDER, "KITCHEN", "Replaced by two smaller jobs"));

            Shelf weekend = shelves.titled("Weekend quick refresh");
            assertThat(weekend.preset().jobs()).containsExactly(JobCode.of("LAUNDRY_FOLD"));
            assertThat(weekend.isShown()).isTrue();
            assertThat(shelves.titled("Kitchen rescue").isShown()).isFalse();
            assertThat(shelves.titled("Laundry day").isShown()).isTrue();
            assertThat(shelves.saves).isEqualTo(savesBefore + 2);
        }

        @Test
        void aJobThatIsNotOnTheListIsNotRetired() {
            assertThatThrownBy(() -> retireJob.retire(new RetireJobRequest(FOUNDER, "KITCHEN", "Replaced")))
                    .isInstanceOf(JobNotFoundException.class);

            assertThat(jobLists.saves).isZero();
            assertThat(events.all).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }
    }
}
