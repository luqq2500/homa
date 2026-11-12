package catalogue.domain;

import static catalogue.domain.CatalogueBuilders.KITCHEN;
import static catalogue.domain.CatalogueBuilders.aJobList;
import static catalogue.domain.CatalogueBuilders.job;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.CatalogueNameRequiredException;
import catalogue.domain.exception.DuplicateJobCodeException;
import catalogue.domain.exception.JobAlreadyRetiredException;
import catalogue.domain.exception.JobNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Feature: Job list. */
class JobListCurationTest {

    @Nested
    class AddingJobs {

        @Test
        void jobCodesAreUnique() {
            JobList list = aJobList().with("KITCHEN_GREASE").build();

            assertThatThrownBy(() -> list.add(job("KITCHEN_GREASE"), "Degrease the kitchen", KITCHEN))
                    .isInstanceOf(DuplicateJobCodeException.class)
                    .hasMessageContaining("KITCHEN_GREASE");
        }

        @Test
        void aNewJobIsOfferedWithItsNameAndGroup() {
            JobList list = JobList.create();

            list.add(job("KITCHEN_GREASE"), "  Degrease the kitchen ", KITCHEN);

            assertThat(list.isOffered(job("KITCHEN_GREASE"))).isTrue();
            Job added = list.job(job("KITCHEN_GREASE")).orElseThrow();
            assertThat(added.code()).isEqualTo(job("KITCHEN_GREASE"));
            assertThat(added.name()).isEqualTo("Degrease the kitchen");
            assertThat(added.group()).isEqualTo(KITCHEN);
            assertThat(added.isActive()).isTrue();
        }

        @Test
        void aJobNeedsAName() {
            JobList list = JobList.create();

            assertThatThrownBy(() -> list.add(job("KITCHEN_GREASE"), " ", KITCHEN))
                    .isInstanceOf(CatalogueNameRequiredException.class)
                    .hasMessage("A job needs a name");
            assertThat(list.contains(job("KITCHEN_GREASE"))).isFalse();
        }

        @Test
        void jobsAreListedInTheOrderHomaAddedThem() {
            JobList list = aJobList().with("TOILET_CARE", "KITCHEN_GREASE", "LAUNDRY_FOLD").build();

            assertThat(list.jobs()).extracting(Job::code)
                    .containsExactly(job("TOILET_CARE"), job("KITCHEN_GREASE"), job("LAUNDRY_FOLD"));
        }

        @Test
        void theListShownCannotBeChangedFromOutside() {
            JobList list = aJobList().with("KITCHEN_GREASE").build();

            assertThatThrownBy(() -> list.jobs().clear()).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> list.offeredCodes().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void eachJobListHasItsOwnId() {
            assertThat(JobList.create().id()).isNotBlank().isNotEqualTo(JobList.create().id());
        }
    }

    @Nested
    class RegroupingJobs {

        @Test
        void aJobMovesToAnotherGroupAndKeepsItsCodeAndName() {
            JobList list = aJobList().with("SOFA_VACUUM").build();

            list.regroup(job("SOFA_VACUUM"), JobGroup.of("Living room"));

            Job moved = list.job(job("SOFA_VACUUM")).orElseThrow();
            assertThat(moved.group()).isEqualTo(JobGroup.of("Living room"));
            assertThat(moved.name()).isEqualTo("Job SOFA_VACUUM");
            assertThat(list.isOffered(job("SOFA_VACUUM"))).isTrue();
        }

        @Test
        void aJobNeedsAGroupToMoveTo() {
            JobList list = aJobList().with("SOFA_VACUUM").build();

            assertThatThrownBy(() -> list.regroup(job("SOFA_VACUUM"), null)).isInstanceOf(NullPointerException.class);
            assertThat(list.job(job("SOFA_VACUUM")).orElseThrow().group()).isEqualTo(CatalogueBuilders.KITCHEN);
        }

        @Test
        void onlyAJobOnTheListCanBeRegrouped() {
            JobList list = aJobList().build();

            assertThatThrownBy(() -> list.regroup(job("SOFA_VACUUM"), JobGroup.of("Living room")))
                    .isInstanceOf(JobNotFoundException.class);
        }
    }

    @Nested
    class RenamingJobs {

        @Test
        void aRenamedJobKeepsItsCode() {
            JobList list = aJobList().with("KITCHEN_GREASE").build();

            list.rename(job("KITCHEN_GREASE"), "Kitchen degreasing");

            assertThat(list.job(job("KITCHEN_GREASE")).orElseThrow().name()).isEqualTo("Kitchen degreasing");
            assertThat(list.isOffered(job("KITCHEN_GREASE"))).isTrue();
        }

        @Test
        void aRenamedJobStillNeedsAName() {
            JobList list = aJobList().with("KITCHEN_GREASE").build();

            assertThatThrownBy(() -> list.rename(job("KITCHEN_GREASE"), ""))
                    .isInstanceOf(CatalogueNameRequiredException.class);
        }

        @Test
        void onlyAJobOnTheListCanBeRenamed() {
            JobList list = JobList.create();

            assertThatThrownBy(() -> list.rename(job("KITCHEN_GREASE"), "Kitchen degreasing"))
                    .isInstanceOf(JobNotFoundException.class)
                    .hasMessageContaining("KITCHEN_GREASE");
        }
    }

    @Nested
    class RetiringJobs {

        @Test
        void aRetiredJobStaysInTheListButIsNoLongerOffered() {
            JobList list = aJobList().with("WARDROBE_ORGANISE").build();

            list.retire(job("WARDROBE_ORGANISE"));

            assertThat(list.contains(job("WARDROBE_ORGANISE"))).isTrue();
            assertThat(list.isOffered(job("WARDROBE_ORGANISE"))).isFalse();
            assertThat(list.job(job("WARDROBE_ORGANISE")).orElseThrow().isActive()).isFalse();
        }

        @Test
        void retiringOneJobLeavesTheOthersOffered() {
            JobList list = aJobList().with("WARDROBE_ORGANISE", "KITCHEN_GREASE").build();

            list.retire(job("WARDROBE_ORGANISE"));

            assertThat(list.offeredCodes()).containsExactly(job("KITCHEN_GREASE"));
            assertThat(list.jobs()).hasSize(2);
        }

        @Test
        void aRetiredJobCodeCannotBeUsedAgain() {
            JobList list = aJobList().with("WARDROBE_ORGANISE").build();
            list.retire(job("WARDROBE_ORGANISE"));

            assertThatThrownBy(() -> list.add(job("WARDROBE_ORGANISE"), "Organise wardrobe", KITCHEN))
                    .isInstanceOf(DuplicateJobCodeException.class);
        }

        @Test
        void aJobIsRetiredOnlyOnce() {
            JobList list = aJobList().with("WARDROBE_ORGANISE").build();
            list.retire(job("WARDROBE_ORGANISE"));

            assertThatThrownBy(() -> list.retire(job("WARDROBE_ORGANISE")))
                    .isInstanceOf(JobAlreadyRetiredException.class)
                    .hasMessageContaining("WARDROBE_ORGANISE");
        }

        @Test
        void onlyAJobOnTheListCanBeRetired() {
            JobList list = JobList.create();

            assertThatThrownBy(() -> list.retire(job("WARDROBE_ORGANISE")))
                    .isInstanceOf(JobNotFoundException.class);
        }

        @Test
        void aJobNotOnTheListIsNotOffered() {
            assertThat(JobList.create().isOffered(job("DECLUTTER_GARAGE"))).isFalse();
            assertThat(JobList.create().job(job("DECLUTTER_GARAGE"))).isEmpty();
        }
    }
}
