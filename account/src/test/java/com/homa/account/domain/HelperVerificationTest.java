package com.homa.account.domain;

import static com.homa.account.domain.AccountBuilders.aHelperNamed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.account.domain.exception.AccountDetailRequiredException;
import com.homa.account.domain.exception.EkycResultNotFinalException;
import com.homa.account.domain.exception.HelperPhotoRequiredException;
import com.homa.account.domain.exception.HelperRemovedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Feature: Helpers. */
class HelperVerificationTest {

    @Nested
    class AddingHelpers {

        @Test
        void aNewHelperStartsUnverified() {
            Helper aina = Helper.add("provider-1", " Aina ", Photo.of("photos/aina.jpg"));

            assertThat(aina.ekycStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(aina.name()).isEqualTo("Aina");
            assertThat(aina.photo()).isEqualTo(Photo.of("photos/aina.jpg"));
            assertThat(aina.providerId()).isEqualTo("provider-1");
            assertThat(aina.id()).isNotBlank();
            assertThat(aina.isOnTeam()).isTrue();
            assertThat(aina.isVerifiedTeamMember()).isFalse();
        }

        @Test
        void aHelperNeedsAPhoto() {
            assertThatThrownBy(() -> Helper.add("provider-1", "Aina", null))
                    .isInstanceOf(HelperPhotoRequiredException.class);
        }

        @Test
        void aHelperNeedsAName() {
            assertThatThrownBy(() -> Helper.add("provider-1", " ", Photo.of("photos/aina.jpg")))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A helper's name is required");
        }

        @Test
        void aHelperBelongsToAProvider() {
            assertThatThrownBy(() -> Helper.add(null, "Aina", Photo.of("photos/aina.jpg")))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A helper's provider is required");
        }
    }

    @Nested
    class VerifyingHelpers {

        @Test
        void aVerifiedHelperCanBeNamedForJobs() {
            Helper aina = aHelperNamed("Aina");

            aina.recordEkycResult(VerificationStatus.VERIFIED);

            assertThat(aina.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(aina.isVerifiedTeamMember()).isTrue();
        }

        @Test
        void aRejectedHelperCannotBeNamedForJobs() {
            Helper aina = aHelperNamed("Aina");

            aina.recordEkycResult(VerificationStatus.REJECTED);

            assertThat(aina.isVerifiedTeamMember()).isFalse();
        }

        @Test
        void pendingIsNotAnEkycResult() {
            assertThatThrownBy(() -> aHelperNamed("Aina").recordEkycResult(VerificationStatus.PENDING))
                    .isInstanceOf(EkycResultNotFinalException.class);
        }
    }

    @Nested
    class RemovingHelpers {

        @Test
        void aRemovedHelperIsOffTheTeamEvenIfVerified() {
            Helper aina = aHelperNamed("Aina");
            aina.recordEkycResult(VerificationStatus.VERIFIED);

            aina.remove();

            assertThat(aina.isOnTeam()).isFalse();
            assertThat(aina.isVerifiedTeamMember()).isFalse();
            assertThat(aina.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
        }

        @Test
        void aRemovedHelperCannotBeRemovedAgainOrVerified() {
            Helper aina = aHelperNamed("Aina");
            aina.remove();

            assertThatThrownBy(aina::remove).isInstanceOf(HelperRemovedException.class);
            assertThatThrownBy(() -> aina.recordEkycResult(VerificationStatus.VERIFIED))
                    .isInstanceOf(HelperRemovedException.class);
        }
    }
}
