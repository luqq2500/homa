package com.homa.conversation.domain;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.CHECK_IN_START;
import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.domain.ConversationFixtures.MONDAY_9_00;
import static com.homa.conversation.domain.ConversationFixtures.PROTECTION_WINDOW_END;
import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAYS_RESERVATION;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_40;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_50;
import static com.homa.conversation.domain.ConversationFixtures.SUNDAY_MORNING;
import static com.homa.conversation.domain.ConversationFixtures.WEDNESDAY_9_00;
import static com.homa.conversation.domain.ConversationFixtures.requestedWindow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReservationChatWindowTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    @Test
    @Tag("CHT-2")
    @DisplayName("Kemas Home messages \"We're at the guardhouse\" at 9:40, and Aisyah messages about a missed corner "
            + "on Sunday morning: the chat is open from Requested until the Protection Window ends.")
    void aRequestedReservationsChatIsOpenFromRequestedUntilItsProtectionWindowEnds() {
        ReservationChatWindow window = requestedWindow();

        for (Instant moment : new Instant[] {REQUESTED_AT, WEDNESDAY_9_00, SATURDAY_9_40, SUNDAY_MORNING}) {
            ChatAccess access = window.accessAt(moment);
            assertThat(access.isOpen()).as("open at %s", moment).isTrue();
            assertThat(access.readOnlyReason()).isEmpty();
            assertThat(access.openUntil()).contains(PROTECTION_WINDOW_END);
        }
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("One minute before the Protection Window ends at Sunday 14:00, the chat is still open.")
    void theChatIsOpenOneMinuteBeforeTheProtectionWindowEnds() {
        assertThat(requestedWindow().accessAt(PROTECTION_WINDOW_END.minus(ONE_MINUTE)).isOpen()).isTrue();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("At Sunday 14:00 the Protection Window has ended, so the chat is read-only from that moment.")
    void theChatIsReadOnlyAtTheMomentTheProtectionWindowEnds() {
        ChatAccess access = requestedWindow().accessAt(PROTECTION_WINDOW_END);

        assertThat(access.isOpen()).isFalse();
        assertThat(access.readOnlyReason()).contains(ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);
        assertThat(access.openUntil()).isEmpty();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("One minute after the Protection Window ends, the chat is read-only.")
    void theChatIsReadOnlyOneMinuteAfterTheProtectionWindowEnds() {
        ChatAccess access = requestedWindow().accessAt(PROTECTION_WINDOW_END.plus(ONE_MINUTE));

        assertThat(access.readOnlyReason()).contains(ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah can read Saturday's chat on Monday but not reply.")
    void theChatCanBeReadButNotWrittenToOnMondayAfterTheProtectionWindowEnded() {
        ChatAccess access = requestedWindow().accessAt(MONDAY_9_00);

        assertThat(access.isOpen()).isFalse();
        assertThat(access.readOnlyReason()).contains(ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);
    }

    static Stream<Arguments> endingsBeforeCheckIn() {
        return Stream.of(
                Arguments.of("withdrawn", (Function<ReservationChatWindow, ReservationChatWindow>)
                        ReservationChatWindow::withdrawn, ChatWindowStatus.WITHDRAWN,
                        ChatReadOnlyReason.RESERVATION_WITHDRAWN),
                Arguments.of("declined", (Function<ReservationChatWindow, ReservationChatWindow>)
                        ReservationChatWindow::declined, ChatWindowStatus.DECLINED,
                        ChatReadOnlyReason.RESERVATION_DECLINED),
                Arguments.of("expired", (Function<ReservationChatWindow, ReservationChatWindow>)
                        ReservationChatWindow::expired, ChatWindowStatus.EXPIRED,
                        ChatReadOnlyReason.RESERVATION_EXPIRED));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endingsBeforeCheckIn")
    @Tag("CHT-5")
    @DisplayName("Aisyah's reservation is Withdrawn, Declined or Expired: its chat is read-only at once, though its "
            + "Protection Window would not have started.")
    void aChatIsReadOnlyAtOnceWhenItsReservationIsWithdrawnDeclinedOrExpired(String ending,
            Function<ReservationChatWindow, ReservationChatWindow> ends, ChatWindowStatus status,
            ChatReadOnlyReason reason) {
        ReservationChatWindow ended = ends.apply(requestedWindow());

        assertThat(ended.status()).isEqualTo(status);
        assertThat(ended.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        for (Instant moment : new Instant[] {REQUESTED_AT, SATURDAY_9_40, MONDAY_9_00}) {
            ChatAccess access = ended.accessAt(moment);
            assertThat(access.isOpen()).as("%s, open at %s", ending, moment).isFalse();
            assertThat(access.readOnlyReason()).contains(reason);
            assertThat(access.openUntil()).isEmpty();
        }
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah cancels on Wednesday: the chat is read-only from then.")
    void aChatIsReadOnlyAtOnceWhenItsReservationIsCancelledBeforeCheckIn() {
        ReservationChatWindow cancelled = requestedWindow().cancelled(WEDNESDAY_9_00, CHECK_IN_START);

        assertThat(cancelled.status()).isEqualTo(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN);
        ChatAccess access = cancelled.accessAt(WEDNESDAY_9_00);
        assertThat(access.isOpen()).isFalse();
        assertThat(access.readOnlyReason()).contains(ChatReadOnlyReason.RESERVATION_CANCELLED_BEFORE_CHECK_IN);
        assertThat(cancelled.accessAt(SATURDAY_9_40).isOpen()).isFalse();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah cancels at 9:50 on Saturday, once the Check-in Period has started: the chat stays open "
            + "until Sunday 14:00.")
    void aChatStaysOpenUntilTheProtectionWindowEndsWhenItsReservationIsCancelledAfterCheckInStarted() {
        ReservationChatWindow cancelled = requestedWindow().cancelled(SATURDAY_9_50, CHECK_IN_START);

        assertThat(cancelled.status()).isEqualTo(ChatWindowStatus.OPEN);
        assertThat(cancelled.accessAt(SATURDAY_9_50).isOpen()).isTrue();
        assertThat(cancelled.accessAt(SUNDAY_MORNING).openUntil()).contains(PROTECTION_WINDOW_END);
        assertThat(cancelled.accessAt(PROTECTION_WINDOW_END.minus(ONE_MINUTE)).isOpen()).isTrue();
        assertThat(cancelled.accessAt(PROTECTION_WINDOW_END).readOnlyReason())
                .contains(ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A cancellation one minute before the Check-in Period starts at 9:45 turns the chat read-only.")
    void aCancellationOneMinuteBeforeCheckInStartsEndsTheChat() {
        ReservationChatWindow cancelled = requestedWindow().cancelled(CHECK_IN_START.minus(ONE_MINUTE), CHECK_IN_START);

        assertThat(cancelled.status()).isEqualTo(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A cancellation at the very moment the Check-in Period starts is not before it: the chat stays open.")
    void aCancellationAtTheMomentCheckInStartsLeavesTheChatOpen() {
        ReservationChatWindow cancelled = requestedWindow().cancelled(CHECK_IN_START, CHECK_IN_START);

        assertThat(cancelled.status()).isEqualTo(ChatWindowStatus.OPEN);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A cancellation one minute after the Check-in Period starts leaves the chat open until Sunday 14:00.")
    void aCancellationOneMinuteAfterCheckInStartsLeavesTheChatOpen() {
        ReservationChatWindow cancelled = requestedWindow().cancelled(CHECK_IN_START.plus(ONE_MINUTE), CHECK_IN_START);

        assertThat(cancelled.status()).isEqualTo(ChatWindowStatus.OPEN);
        assertThat(cancelled.accessAt(SUNDAY_MORNING).isOpen()).isTrue();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Whichever ending is heard first stands: a withdrawn reservation's chat is not changed by a later "
            + "cancellation, and an ended chat never reopens.")
    void theFirstEndingOfAReservationStands() {
        ReservationChatWindow withdrawn = requestedWindow().withdrawn();

        assertThat(withdrawn.cancelled(WEDNESDAY_9_00, CHECK_IN_START).status()).isEqualTo(ChatWindowStatus.WITHDRAWN);
        assertThat(withdrawn.declined().status()).isEqualTo(ChatWindowStatus.WITHDRAWN);
        assertThat(withdrawn.expired().status()).isEqualTo(ChatWindowStatus.WITHDRAWN);
        assertThat(requestedWindow().declined().withdrawn().status()).isEqualTo(ChatWindowStatus.DECLINED);
        assertThat(requestedWindow().expired().declined().status()).isEqualTo(ChatWindowStatus.EXPIRED);
        assertThat(requestedWindow().cancelled(WEDNESDAY_9_00, CHECK_IN_START).expired().status())
                .isEqualTo(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN);
        assertThat(withdrawn.cancelled(SATURDAY_9_50, CHECK_IN_START).accessAt(SATURDAY_9_50).isOpen()).isFalse();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A withdrawal heard after the Protection Window would have ended is still a withdrawal.")
    void anEndedChatNamesItsEndingEvenAfterTheProtectionWindowPassed() {
        ChatAccess access = requestedWindow().expired().accessAt(MONDAY_9_00);

        assertThat(access.readOnlyReason()).contains(ChatReadOnlyReason.RESERVATION_EXPIRED);
    }

    static Stream<Arguments> endingsHeardBeforeTheRequest() {
        return Stream.of(
                Arguments.of("withdrawn", (Supplier<ReservationChatWindow>) () -> ReservationChatWindow
                        .withdrawnBeforeRequested(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME),
                        ChatWindowStatus.WITHDRAWN, ChatReadOnlyReason.RESERVATION_WITHDRAWN),
                Arguments.of("declined", (Supplier<ReservationChatWindow>) () -> ReservationChatWindow
                        .declinedBeforeRequested(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME),
                        ChatWindowStatus.DECLINED, ChatReadOnlyReason.RESERVATION_DECLINED),
                Arguments.of("expired", (Supplier<ReservationChatWindow>) () -> ReservationChatWindow
                        .expiredBeforeRequested(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME),
                        ChatWindowStatus.EXPIRED, ChatReadOnlyReason.RESERVATION_EXPIRED));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endingsHeardBeforeTheRequest")
    @Tag("CHT-5")
    @DisplayName("A Withdrawn, Declined or Expired reservation is heard of before its request: the end is kept, the "
            + "chat is read-only, and the request later fills in the Protection Window end without reopening it.")
    void anEndingHeardBeforeTheRequestIsKeptAndTheRequestFillsInTheWindowEnd(String ending,
            Supplier<ReservationChatWindow> heard, ChatWindowStatus status, ChatReadOnlyReason reason) {
        ReservationChatWindow early = heard.get();

        assertThat(early.reservationId()).isEqualTo(SATURDAYS_RESERVATION);
        assertThat(early.customerId()).isEqualTo(AISYAH);
        assertThat(early.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(early.status()).isEqualTo(status);
        assertThat(early.protectionWindowEnd()).isEmpty();
        assertThat(early.accessAt(REQUESTED_AT).readOnlyReason()).contains(reason);

        ReservationChatWindow requested = early.withProtectionWindowEnd(PROTECTION_WINDOW_END);

        assertThat(requested.status()).isEqualTo(status);
        assertThat(requested.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        assertThat(requested.accessAt(REQUESTED_AT).readOnlyReason()).contains(reason);
        assertThat(requested.reservationId()).isEqualTo(SATURDAYS_RESERVATION);
        assertThat(requested.customerId()).isEqualTo(AISYAH);
        assertThat(requested.providerId()).isEqualTo(KEMAS_HOME);
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("A request heard again with the Protection Window end replaces the end, and the chat stays open.")
    void theRequestSetsTheProtectionWindowEnd() {
        Instant later = PROTECTION_WINDOW_END.plus(Duration.ofHours(1));

        ReservationChatWindow window = requestedWindow().withProtectionWindowEnd(later);

        assertThat(window.status()).isEqualTo(ChatWindowStatus.OPEN);
        assertThat(window.protectionWindowEnd()).contains(later);
        assertThat(window.accessAt(PROTECTION_WINDOW_END).isOpen()).isTrue();
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("A reservation's chat window names the reservation, the customer and the provider.")
    void aWindowNamesItsReservationAndBothParties() {
        ReservationChatWindow window = requestedWindow();

        assertThat(window.reservationId()).isEqualTo(SATURDAYS_RESERVATION);
        assertThat(window.customerId()).isEqualTo(AISYAH);
        assertThat(window.providerId()).isEqualTo(KEMAS_HOME);
    }

    @Test
    @DisplayName("An ending leaves the window's reservation and parties as they were.")
    void anEndingKeepsTheReservationAndItsParties() {
        ReservationChatWindow ended = requestedWindow().withdrawn();

        assertThat(ended.reservationId()).isEqualTo(SATURDAYS_RESERVATION);
        assertThat(ended.customerId()).isEqualTo(AISYAH);
        assertThat(ended.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(requestedWindow().withProtectionWindowEnd(PROTECTION_WINDOW_END).reservationId())
                .isEqualTo(SATURDAYS_RESERVATION);
    }

    @Test
    @DisplayName("A chat that is open needs the end of its Protection Window.")
    void anOpenWindowNeedsItsProtectionWindowEnd() {
        assertThatThrownBy(() -> ReservationChatWindow.open(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> requestedWindow().withProtectionWindowEnd(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("A window needs its reservation and both parties.")
    void aWindowNeedsItsReservationAndBothParties() {
        assertThatThrownBy(() -> ReservationChatWindow.open(null, AISYAH, KEMAS_HOME, PROTECTION_WINDOW_END))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ReservationChatWindow.open(SATURDAYS_RESERVATION, null, KEMAS_HOME,
                PROTECTION_WINDOW_END)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ReservationChatWindow.open(SATURDAYS_RESERVATION, AISYAH, null,
                PROTECTION_WINDOW_END)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ReservationChatWindow.restore(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME, null,
                PROTECTION_WINDOW_END)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Every stored window is rebuilt as it was: open with its end, or ended with or without one.")
    void aStoredWindowIsRebuiltAsItWas() {
        ReservationChatWindow open = ReservationChatWindow.restore(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME,
                ChatWindowStatus.OPEN, PROTECTION_WINDOW_END);
        ReservationChatWindow ended = ReservationChatWindow.restore(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME,
                ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN, PROTECTION_WINDOW_END);
        ReservationChatWindow endedWithoutEnd = ReservationChatWindow.restore(SATURDAYS_RESERVATION, AISYAH,
                KEMAS_HOME, ChatWindowStatus.DECLINED, null);

        assertThat(open.status()).isEqualTo(ChatWindowStatus.OPEN);
        assertThat(open.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        assertThat(open.accessAt(SATURDAY_9_40).isOpen()).isTrue();
        assertThat(ended.status()).isEqualTo(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN);
        assertThat(ended.accessAt(SATURDAY_9_40).isOpen()).isFalse();
        assertThat(endedWithoutEnd.protectionWindowEnd()).isEmpty();
        assertThat(endedWithoutEnd.accessAt(SATURDAY_9_40).readOnlyReason())
                .contains(ChatReadOnlyReason.RESERVATION_DECLINED);
    }

    @Test
    @DisplayName("A stored window that is open without the end of its Protection Window is refused.")
    void aStoredOpenWindowWithoutItsEndIsRefused() {
        assertThatThrownBy(() -> ReservationChatWindow.restore(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME,
                ChatWindowStatus.OPEN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Protection Window end");
    }
}
