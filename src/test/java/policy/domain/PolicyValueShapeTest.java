package policy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kernel.Percentage;
import policy.domain.PolicyValue.BookingHorizon;
import policy.domain.PolicyValue.CancellationTierFee;
import policy.domain.PolicyValue.CheckInPeriod;
import policy.domain.PolicyValue.CheckInRadius;
import policy.domain.PolicyValue.CompletionDeadline;
import policy.domain.PolicyValue.CompletionReminder;
import policy.domain.PolicyValue.CustomerNoShowFee;
import policy.domain.PolicyValue.CustomerNoShowWait;
import policy.domain.PolicyValue.DisputeWindowExtension;
import policy.domain.PolicyValue.DurationRange;
import policy.domain.PolicyValue.EarliestCheckIn;
import policy.domain.PolicyValue.EvidenceRetention;
import policy.domain.PolicyValue.FeeWindow;
import policy.domain.PolicyValue.FineDeductionCap;
import policy.domain.PolicyValue.AcceptedDocuments;
import policy.domain.PolicyValue.FaceCheckAttempts;
import policy.domain.PolicyValue.VerificationSessionExpiry;
import policy.domain.PolicyValue.MinimumNotice;
import policy.domain.PolicyValue.MinimumRatingsToShow;
import policy.domain.PolicyValue.NoShowReportWindow;
import policy.domain.PolicyValue.ProviderLateCancellationFine;
import policy.domain.PolicyValue.ProviderNoShowFine;
import policy.domain.PolicyValue.RequestExpiry;
import policy.domain.PolicyValue.EarlyRecheckDeadline;
import policy.domain.PolicyValue.ReverificationPeriod;
import policy.domain.PolicyValue.ReverificationReminder;
import policy.domain.PolicyValue.RatingWindow;
import policy.domain.PolicyValue.ServiceFee;
import policy.domain.PolicyValue.TeamReminder;
import policy.domain.exception.CheckInRadiusNotPositiveException;
import policy.domain.exception.FaceCheckAttemptsNotPositiveException;
import policy.domain.exception.ForeignDocumentNotAllowedException;
import policy.domain.exception.InvalidCountryCodeException;
import policy.domain.exception.NoAcceptedDocumentException;
import policy.domain.exception.InvalidPolicyRangeException;
import policy.domain.exception.MinimumRatingsToShowNotPositiveException;
import policy.domain.exception.PolicyPeriodNotPositiveException;
import policy.domain.exception.ReverificationPeriodNotPositiveException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** The shape each Homa-set value must have, before any rule across values. */
class PolicyValueShapeTest {

    @Nested
    class Periods {

        static Stream<Named<Function<Duration, PolicyValue>>> periods() {
            return Stream.of(
                    Named.of("minimum notice", MinimumNotice::new),
                    Named.of("booking horizon", BookingHorizon::new),
                    Named.of("request expiry", RequestExpiry::new),
                    Named.of("fee window", FeeWindow::new),
                    Named.of("team reminder", TeamReminder::new),
                    Named.of("earliest check-in", EarliestCheckIn::new),
                    Named.of("check-in period", CheckInPeriod::new),
                    Named.of("customer no-show wait", CustomerNoShowWait::new),
                    Named.of("no-show report window", NoShowReportWindow::new),
                    Named.of("completion reminder", CompletionReminder::new),
                    Named.of("completion deadline", CompletionDeadline::new),
                    Named.of("dispute window extension", DisputeWindowExtension::new),
                    Named.of("evidence retention", EvidenceRetention::new),
                    Named.of("rating window", RatingWindow::new),
                    Named.of("re-verification reminder", ReverificationReminder::new),
                    Named.of("early re-check deadline", EarlyRecheckDeadline::new),
                    Named.of("verification session expiry", VerificationSessionExpiry::new));
        }

        @ParameterizedTest(name = "the {0} must be longer than zero")
        @MethodSource("periods")
        void everyHomaSetPeriodIsLongerThanZero(Function<Duration, PolicyValue> period) {
            assertThatThrownBy(() -> period.apply(Duration.ZERO)).isInstanceOf(PolicyPeriodNotPositiveException.class);
            assertThatThrownBy(() -> period.apply(Duration.ofMinutes(-15)))
                    .isInstanceOf(PolicyPeriodNotPositiveException.class);
        }

        @ParameterizedTest(name = "the {0} can be as short as a second")
        @MethodSource("periods")
        void anyPeriodLongerThanZeroIsAccepted(Function<Duration, PolicyValue> period) {
            assertThatCode(() -> period.apply(Duration.ofSeconds(1))).doesNotThrowAnyException();
        }

        @Test
        void aRejectedPeriodIsNamedInTheMessage() {
            assertThatThrownBy(() -> new CheckInPeriod(Duration.ZERO))
                    .hasMessage("The check-in period must be longer than zero, was PT0S");
        }
    }

    @Nested
    class Ranges {

        static Stream<Named<BiFunction<Integer, Integer, PolicyValue>>> ranges() {
            return Stream.of(
                    Named.of("duration range", DurationRange::new));
        }

        @ParameterizedTest(name = "the {0} must start at 1 or more")
        @MethodSource("ranges")
        void aRangeStartsAtOneOrMore(BiFunction<Integer, Integer, PolicyValue> range) {
            assertThatThrownBy(() -> range.apply(0, 6)).isInstanceOf(InvalidPolicyRangeException.class);
            assertThatCode(() -> range.apply(1, 6)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "the {0} minimum cannot be above its maximum")
        @MethodSource("ranges")
        void aRangeMinimumIsNotAboveItsMaximum(BiFunction<Integer, Integer, PolicyValue> range) {
            assertThatThrownBy(() -> range.apply(4, 3)).isInstanceOf(InvalidPolicyRangeException.class);
            assertThatCode(() -> range.apply(3, 3)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0} to {1} hours")
        @CsvSource({"1, 6", "2, 4"})
        void aRangeKeepsItsBounds(int min, int max) {
            assertThat(new DurationRange(min, max).minHours()).isEqualTo(min);
            assertThat(new DurationRange(min, max).maxHours()).isEqualTo(max);
        }

        @Test
        void aRejectedRangeIsShownInTheMessage() {
            assertThatThrownBy(() -> new DurationRange(4, 3)).hasMessageContaining("4 to 3");
        }
    }

    @Test
    void averagesAreShownAfterAtLeastOneRating() {
        assertThatThrownBy(() -> new MinimumRatingsToShow(0)).isInstanceOf(MinimumRatingsToShowNotPositiveException.class);
        assertThat(new MinimumRatingsToShow(1).ratings()).isEqualTo(1);
    }

    @Test
    void reverificationIsDueAtLeastAMonthAfterTheLastVerification() {
        assertThatThrownBy(() -> new ReverificationPeriod(0))
                .isInstanceOf(ReverificationPeriodNotPositiveException.class)
                .hasMessageContaining("was 0");
        assertThat(new ReverificationPeriod(1).months()).isEqualTo(1);
    }

    @Test
    void everyoneGetsAtLeastOneFaceCheckAttempt() {
        assertThatThrownBy(() -> new FaceCheckAttempts(0))
                .isInstanceOf(FaceCheckAttemptsNotPositiveException.class)
                .hasMessageContaining("was 0");
        assertThat(new FaceCheckAttempts(1).attempts()).isEqualTo(1);
    }

    @Nested
    class AcceptedDocumentsList {

        private static final AcceptedDocument PROVIDER_MYKAD =
                AcceptedDocument.issuedBy(DocumentHolder.PROVIDER, DocumentType.IDENTITY_CARD, "MY");
        private static final AcceptedDocument INDEPENDENT_MEMBER_MYKAD =
                AcceptedDocument.issuedBy(DocumentHolder.INDEPENDENT_TEAM_MEMBER, DocumentType.IDENTITY_CARD, "MY");
        private static final AcceptedDocument BUSINESS_MEMBER_ANY_PASSPORT =
                AcceptedDocument.fromAnyCountry(DocumentHolder.BUSINESS_TEAM_MEMBER, DocumentType.PASSPORT);

        @Test
        void everyKindOfPersonNeedsAnAcceptedDocument() {
            assertThatThrownBy(() -> new AcceptedDocuments(Set.of(PROVIDER_MYKAD, INDEPENDENT_MEMBER_MYKAD)))
                    .isInstanceOf(NoAcceptedDocumentException.class)
                    .hasMessageContaining("BUSINESS_TEAM_MEMBER");
            assertThatThrownBy(() -> new AcceptedDocuments(Set.of(PROVIDER_MYKAD, BUSINESS_MEMBER_ANY_PASSPORT)))
                    .isInstanceOf(NoAcceptedDocumentException.class)
                    .hasMessageContaining("INDEPENDENT_TEAM_MEMBER");
            assertThatThrownBy(() -> new AcceptedDocuments(Set.of(INDEPENDENT_MEMBER_MYKAD, BUSINESS_MEMBER_ANY_PASSPORT)))
                    .isInstanceOf(NoAcceptedDocumentException.class)
                    .hasMessageContaining("PROVIDER");
        }

        @Test
        void aListCoveringEveryoneIsAccepted() {
            AcceptedDocuments list = new AcceptedDocuments(
                    Set.of(PROVIDER_MYKAD, INDEPENDENT_MEMBER_MYKAD, BUSINESS_MEMBER_ANY_PASSPORT));

            assertThat(list.documents()).hasSize(3);
        }

        @ParameterizedTest(name = "a foreign document for {0} is refused")
        @EnumSource(value = DocumentHolder.class, names = {"PROVIDER", "INDEPENDENT_TEAM_MEMBER"})
        void providersAndIndependentMembersCanOnlyUseMalaysianDocuments(DocumentHolder holder) {
            Set<AcceptedDocument> foreign = new HashSet<>(Set.of(PROVIDER_MYKAD, INDEPENDENT_MEMBER_MYKAD, BUSINESS_MEMBER_ANY_PASSPORT));
            foreign.add(AcceptedDocument.issuedBy(holder, DocumentType.PASSPORT, "ID"));
            Set<AcceptedDocument> anyCountry = new HashSet<>(Set.of(PROVIDER_MYKAD, INDEPENDENT_MEMBER_MYKAD, BUSINESS_MEMBER_ANY_PASSPORT));
            anyCountry.add(AcceptedDocument.fromAnyCountry(holder, DocumentType.PASSPORT));

            assertThatThrownBy(() -> new AcceptedDocuments(foreign))
                    .isInstanceOf(ForeignDocumentNotAllowedException.class)
                    .hasMessageContaining(holder.name());
            assertThatThrownBy(() -> new AcceptedDocuments(anyCountry)).isInstanceOf(ForeignDocumentNotAllowedException.class);
        }

        @Test
        void aBusinessMemberCanUseAForeignDocument() {
            AcceptedDocument indonesian = AcceptedDocument.issuedBy(DocumentHolder.BUSINESS_TEAM_MEMBER, DocumentType.PASSPORT, "ID");

            AcceptedDocuments list = new AcceptedDocuments(Set.of(PROVIDER_MYKAD, INDEPENDENT_MEMBER_MYKAD, indonesian));

            assertThat(list.documents()).contains(indonesian);
        }

        @Test
        void aCountryIsAnIsoCode() {
            assertThatThrownBy(() -> AcceptedDocument.issuedBy(DocumentHolder.PROVIDER, DocumentType.PASSPORT, "Malaysia"))
                    .isInstanceOf(InvalidCountryCodeException.class)
                    .hasMessageContaining("Malaysia");
            assertThat(AcceptedDocument.issuedBy(DocumentHolder.PROVIDER, DocumentType.PASSPORT, "MY").issuingCountry())
                    .contains("MY");
            assertThatThrownBy(() -> new AcceptedDocument(null, DocumentType.PASSPORT, Optional.empty()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new AcceptedDocument(DocumentHolder.PROVIDER, null, Optional.empty()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new AcceptedDocument(DocumentHolder.PROVIDER, DocumentType.PASSPORT, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void theCheckInRadiusIsMoreThanZeroMetres() {
        assertThatThrownBy(() -> new CheckInRadius(0)).isInstanceOf(CheckInRadiusNotPositiveException.class);
        assertThat(new CheckInRadius(1).metres()).isEqualTo(1);
    }

    static Stream<Named<Function<Percentage, PolicyValue>>> shares() {
        return Stream.of(
                Named.of("service fee", ServiceFee::new),
                Named.of("customer no-show fee", CustomerNoShowFee::new),
                Named.of("late cancellation fine", ProviderLateCancellationFine::new),
                Named.of("no-show fine", ProviderNoShowFine::new),
                Named.of("fine deduction cap", FineDeductionCap::new),
                Named.of("Standard tier fee", share -> new CancellationTierFee(CancellationTier.STANDARD, share)));
    }

    @ParameterizedTest(name = "the {0} needs a share")
    @MethodSource("shares")
    void everyShareNeedsAValue(Function<Percentage, PolicyValue> share) {
        assertThatThrownBy(() -> share.apply(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aTierFeeNeedsATier() {
        assertThatThrownBy(() -> new CancellationTierFee(null, Percentage.of(10))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aTierFeeIsKeyedByItsTierAndEveryOtherValueByItsKind() {
        assertThat(new CancellationTierFee(CancellationTier.FIRM, Percentage.of(20)).key()).isEqualTo(CancellationTier.FIRM);
        assertThat(new ServiceFee(Percentage.of(15)).key()).isEqualTo(ServiceFee.class);
    }
}
