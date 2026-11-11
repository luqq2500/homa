package ie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ie.events.CaseDecidedIE;
import ie.events.CheckInCompletedIE;
import ie.events.CompletionDeadlineMissedIE;
import ie.events.CompletionSubmittedIE;
import ie.events.CustomerRegisteredIE;
import ie.events.EnquiryClosedIE;
import ie.events.EnquiryCreatedIE;
import ie.events.EnquiryRequestedIE;
import ie.events.FaceCheckRecordedIE;
import ie.events.FineChargedIE;
import ie.events.FineWaivedIE;
import ie.events.HomaPolicyChangedIE;
import ie.events.JobAddedIE;
import ie.events.JobRetiredIE;
import ie.events.ListingChangedIE;
import ie.events.ListingPausedIE;
import ie.events.ListingResumedIE;
import ie.events.ListingWentLiveIE;
import ie.events.ListingWentOfflineIE;
import ie.events.MemberVerificationRejectedIE;
import ie.events.MemberVerificationStartedIE;
import ie.events.NoShowChallengedIE;
import ie.events.PaymentCapturedIE;
import ie.events.PaymentRefundedIE;
import ie.events.PayoutAccountReadyIE;
import ie.events.PayoutCreatedIE;
import ie.events.ProviderCheckedInIE;
import ie.events.ProviderReverifiedIE;
import ie.events.ProviderVerificationLapsedIE;
import ie.events.ProviderVerificationRejectedIE;
import ie.events.ProviderVerifiedIE;
import ie.events.RatingHiddenIE;
import ie.events.RatingSubmittedIE;
import ie.events.RecheckFailedIE;
import ie.events.ReportFiledIE;
import ie.events.ReservationAcceptedIE;
import ie.events.ReservationCancelledIE;
import ie.events.ReservationCompletedIE;
import ie.events.ReservationDeclinedIE;
import ie.events.ReservationEditedIE;
import ie.events.ReservationExpiredIE;
import ie.events.ReservationRequestedIE;
import ie.events.SettlementDecidedIE;
import ie.events.TeamMemberAddedIE;
import ie.events.TeamMemberLapsedIE;
import ie.events.TeamMemberRemovedIE;
import ie.events.TeamMemberReverifiedIE;
import ie.events.TeamNamedIE;
import ie.events.ToolAddedIE;
import ie.events.ToolRetiredIE;
import ie.port.AccountIE;
import ie.port.CatalogueIE;
import ie.port.FeedbackIE;
import ie.port.OfferingIE;
import ie.port.PaymentIE;
import ie.port.PolicyIE;
import ie.port.ReservationIE;
import ie.port.ResolutionIE;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import kernel.Money;
import kernel.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The integration event contracts. Event records have no logic, so each one is checked for the contract
 * only: it can be built only with every required field, it cannot be changed after it is built, and it
 * belongs to its publishing context's event interface.
 */
class IntegrationEventContractTest {

    private static final Instant OCCURRED = Instant.parse("2026-11-14T02:00:00Z");

    static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of(JobAddedIE.class, CatalogueIE.class),
                Arguments.of(JobRetiredIE.class, CatalogueIE.class),
                Arguments.of(ToolAddedIE.class, CatalogueIE.class),
                Arguments.of(ToolRetiredIE.class, CatalogueIE.class),
                Arguments.of(HomaPolicyChangedIE.class, PolicyIE.class),
                Arguments.of(CustomerRegisteredIE.class, AccountIE.class),
                Arguments.of(ProviderVerifiedIE.class, AccountIE.class),
                Arguments.of(ProviderVerificationRejectedIE.class, AccountIE.class),
                Arguments.of(PayoutAccountReadyIE.class, AccountIE.class),
                Arguments.of(MemberVerificationStartedIE.class, AccountIE.class),
                Arguments.of(MemberVerificationRejectedIE.class, AccountIE.class),
                Arguments.of(TeamMemberAddedIE.class, AccountIE.class),
                Arguments.of(TeamMemberRemovedIE.class, AccountIE.class),
                Arguments.of(RecheckFailedIE.class, AccountIE.class),
                Arguments.of(TeamMemberReverifiedIE.class, AccountIE.class),
                Arguments.of(TeamMemberLapsedIE.class, AccountIE.class),
                Arguments.of(ProviderReverifiedIE.class, AccountIE.class),
                Arguments.of(ProviderVerificationLapsedIE.class, AccountIE.class),
                Arguments.of(ListingWentLiveIE.class, OfferingIE.class),
                Arguments.of(ListingChangedIE.class, OfferingIE.class),
                Arguments.of(ListingWentOfflineIE.class, OfferingIE.class),
                Arguments.of(ListingPausedIE.class, OfferingIE.class),
                Arguments.of(ListingResumedIE.class, OfferingIE.class),
                Arguments.of(EnquiryCreatedIE.class, ReservationIE.class),
                Arguments.of(EnquiryRequestedIE.class, ReservationIE.class),
                Arguments.of(EnquiryClosedIE.class, ReservationIE.class),
                Arguments.of(ReservationRequestedIE.class, ReservationIE.class),
                Arguments.of(ReservationEditedIE.class, ReservationIE.class),
                Arguments.of(ReservationAcceptedIE.class, ReservationIE.class),
                Arguments.of(ReservationDeclinedIE.class, ReservationIE.class),
                Arguments.of(ReservationExpiredIE.class, ReservationIE.class),
                Arguments.of(ReservationCancelledIE.class, ReservationIE.class),
                Arguments.of(TeamNamedIE.class, ReservationIE.class),
                Arguments.of(ProviderCheckedInIE.class, ReservationIE.class),
                Arguments.of(FaceCheckRecordedIE.class, ReservationIE.class),
                Arguments.of(CheckInCompletedIE.class, ReservationIE.class),
                Arguments.of(CompletionSubmittedIE.class, ReservationIE.class),
                Arguments.of(ReportFiledIE.class, ReservationIE.class),
                Arguments.of(NoShowChallengedIE.class, ReservationIE.class),
                Arguments.of(CompletionDeadlineMissedIE.class, ReservationIE.class),
                Arguments.of(ReservationCompletedIE.class, ReservationIE.class),
                Arguments.of(SettlementDecidedIE.class, ReservationIE.class),
                Arguments.of(FineChargedIE.class, ReservationIE.class),
                Arguments.of(CaseDecidedIE.class, ResolutionIE.class),
                Arguments.of(PaymentCapturedIE.class, PaymentIE.class),
                Arguments.of(PaymentRefundedIE.class, PaymentIE.class),
                Arguments.of(PayoutCreatedIE.class, PaymentIE.class),
                Arguments.of(FineWaivedIE.class, PaymentIE.class),
                Arguments.of(RatingSubmittedIE.class, FeedbackIE.class),
                Arguments.of(RatingHiddenIE.class, FeedbackIE.class));
    }

    @ParameterizedTest(name = "{0} is a {1}")
    @MethodSource("events")
    void anEventBelongsToItsPublishersEventInterfaceAndSaysWhenItHappened(Class<? extends Record> event, Class<?> port)
            throws Exception {
        assertThat(port).isAssignableFrom(event);

        Object built = build(event, -1);

        assertThat(event.getMethod("occurredAt").invoke(built)).isEqualTo(OCCURRED);
    }

    @ParameterizedTest(name = "{0} needs every field")
    @MethodSource("events")
    void anEventCanOnlyBeBuiltWithEveryRequiredField(Class<? extends Record> event, Class<?> port) {
        RecordComponent[] components = event.getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i].getType().isPrimitive()) {
                continue;
            }
            int missing = i;
            assertThatThrownBy(() -> build(event, missing))
                    .as("%s without %s", event.getSimpleName(), components[i].getName())
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @ParameterizedTest(name = "{0} cannot be changed after it is built")
    @MethodSource("events")
    void anEventCannotBeChangedAfterItIsBuilt(Class<? extends Record> event, Class<?> port) throws Exception {
        Constructor<?> constructor = canonical(event);
        Object[] values = sampleValues(event, -1);
        Object built = constructor.newInstance(values);

        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Collection<?> given) {
                given.clear();
                Collection<?> kept = (Collection<?>) event.getRecordComponents()[i].getAccessor().invoke(built);
                assertThat(kept).as("%s keeps its %s", event.getSimpleName(), event.getRecordComponents()[i].getName()).hasSize(1);
                assertThatThrownBy(kept::clear).isInstanceOf(UnsupportedOperationException.class);
            }
        }
        assertThat(Arrays.stream(event.getDeclaredFields()).allMatch(field -> java.lang.reflect.Modifier.isFinal(field.getModifiers())))
                .isTrue();
    }

    private static final Set<Class<?>> ALLOWED = Set.of(
            int.class, long.class, boolean.class, String.class, Money.class, Percentage.class,
            Instant.class, Duration.class, List.class, Set.class);

    /**
     * Events carry primitives, Strings, kernel types and JDK value types ({@code Instant},
     * {@code Duration}, immutable {@code List}/{@code Set}); never domain objects; no {@code Optional}
     * fields. Collections hold Strings.
     */
    @ParameterizedTest(name = "{0} carries only allowed types")
    @MethodSource("events")
    void anEventCarriesOnlyPrimitivesStringsKernelAndJdkValueTypes(Class<? extends Record> event, Class<?> port) {
        for (RecordComponent component : event.getRecordComponents()) {
            assertThat(ALLOWED).as("%s.%s", event.getSimpleName(), component.getName()).contains(component.getType());
            if (component.getGenericType() instanceof ParameterizedType collection) {
                assertThat(collection.getActualTypeArguments()).as("%s.%s", event.getSimpleName(), component.getName())
                        .containsExactly(String.class);
            }
        }
    }

    @Test
    void everyEventInThePlanIsHere() {
        assertThat(events().count()).isEqualTo(50);
    }

    private static Object build(Class<? extends Record> event, int missing) throws Exception {
        try {
            return canonical(event).newInstance(sampleValues(event, missing));
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private static Constructor<?> canonical(Class<? extends Record> event) throws NoSuchMethodException {
        Class<?>[] types = Arrays.stream(event.getRecordComponents()).map(RecordComponent::getType).toArray(Class<?>[]::new);
        return event.getDeclaredConstructor(types);
    }

    /** A sample value for every field, or null for the field at {@code missing}. */
    private static Object[] sampleValues(Class<? extends Record> event, int missing) {
        RecordComponent[] components = event.getRecordComponents();
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            values[i] = i == missing ? null : sample(components[i].getType(), components[i].getGenericType(), components[i].getName());
        }
        return values;
    }

    private static Object sample(Class<?> type, Type generic, String name) {
        if (type == int.class) {
            return 1;
        }
        if (type == boolean.class) {
            return true;
        }
        if (type == String.class) {
            return name + "-1";
        }
        if (type == Instant.class) {
            return OCCURRED;
        }
        if (type == Duration.class) {
            return Duration.ofMinutes(15);
        }
        if (type == Money.class) {
            return Money.parse("230.00");
        }
        if (type == Percentage.class) {
            return Percentage.of(15);
        }
        if (type == Set.class) {
            return new HashSet<>(Set.of(name + "-1"));
        }
        if (type == List.class) {
            return new ArrayList<>(List.of(name + "-1"));
        }
        throw new AssertionError("No sample for " + type + " in " + name);
    }
}
