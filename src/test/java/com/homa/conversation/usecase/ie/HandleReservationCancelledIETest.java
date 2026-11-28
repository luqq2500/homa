package com.homa.conversation.usecase.ie;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.CHECK_IN_START;
import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.domain.ConversationFixtures.PROTECTION_WINDOW_END;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAYS_RESERVATION;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_40;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_50;
import static com.homa.conversation.domain.ConversationFixtures.SUNDAY_MORNING;
import static com.homa.conversation.domain.ConversationFixtures.WEDNESDAY_9_00;
import static com.homa.conversation.usecase.ConversationTestBed.AISYAH_ID;
import static com.homa.conversation.usecase.ConversationTestBed.AZMAN_PERSON_ID;
import static com.homa.conversation.usecase.ConversationTestBed.reservationCancelledEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationRequestedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.ChatReadOnlyReason;
import com.homa.conversation.domain.ChatWindowStatus;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.exception.ChatReadOnlyException;
import com.homa.conversation.usecase.ConversationTestBed;
import com.homa.conversation.usecase.dto.SendMessageRequest;
import com.homa.ie.events.ReservationCancelledIE;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HandleReservationCancelledIETest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();

    private ChatWindowStatus windowStatus() {
        return bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow().status();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah cancels on Wednesday, before the Check-in Period: the chat is read-only from then.")
    void aCancellationBeforeCheckInMakesTheChatReadOnlyAtOnce() {
        String chatId = bed.requestSaturdaysReservation();
        bed.time.setNow(WEDNESDAY_9_00);

        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", WEDNESDAY_9_00));

        assertThat(windowStatus()).isEqualTo(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN);
        assertThat(bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow().protectionWindowEnd())
                .contains(PROTECTION_WINDOW_END);
        assertThatThrownBy(() -> bed.sendMessage.send(new SendMessageRequest(chatId, AISYAH_ID, "Sorry")))
                .isInstanceOfSatisfying(ChatReadOnlyException.class, refusal -> assertThat(refusal.reason())
                        .isEqualTo(ChatReadOnlyReason.RESERVATION_CANCELLED_BEFORE_CHECK_IN));
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah cancels at 9:50 on Saturday: the chat stays open until Sunday 14:00, for both of them.")
    void aCancellationAfterCheckInStartedLeavesTheChatOpenUntilTheProtectionWindowEnds() {
        String chatId = bed.requestSaturdaysReservation();

        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", SATURDAY_9_50));

        assertThat(windowStatus()).isEqualTo(ChatWindowStatus.OPEN);
        bed.time.setNow(SATURDAY_9_50);
        assertThat(bed.sendMessage.send(new SendMessageRequest(chatId, AZMAN_PERSON_ID, "The team has left")).text())
                .isEqualTo("The team has left");
        bed.time.setNow(SUNDAY_MORNING);
        assertThat(bed.sendMessage.send(new SendMessageRequest(chatId, AISYAH_ID, "Thanks")).text())
                .isEqualTo("Thanks");
        bed.time.setNow(PROTECTION_WINDOW_END);
        assertThatThrownBy(() -> bed.sendMessage.send(new SendMessageRequest(chatId, AISYAH_ID, "Hello?")))
                .isInstanceOfSatisfying(ChatReadOnlyException.class, refusal -> assertThat(refusal.reason())
                        .isEqualTo(ChatReadOnlyReason.PROTECTION_WINDOW_ENDED));
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A cancellation one minute before the Check-in Period starts at 9:45 ends the chat at once.")
    void aCancellationOneMinuteBeforeCheckInStartsEndsTheChat() {
        bed.requestSaturdaysReservation();

        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", CHECK_IN_START.minus(ONE_MINUTE)));

        assertThat(windowStatus()).isEqualTo(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A cancellation at the very moment the Check-in Period starts is not before it: the chat stays open.")
    void aCancellationAtTheMomentCheckInStartsLeavesTheChatOpen() {
        bed.requestSaturdaysReservation();

        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", CHECK_IN_START));

        assertThat(windowStatus()).isEqualTo(ChatWindowStatus.OPEN);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A cancellation one minute after the Check-in Period starts leaves the chat open.")
    void aCancellationOneMinuteAfterCheckInStartsLeavesTheChatOpen() {
        bed.requestSaturdaysReservation();

        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", CHECK_IN_START.plus(ONE_MINUTE)));

        assertThat(windowStatus()).isEqualTo(ChatWindowStatus.OPEN);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"CUSTOMER_CANCELLED", "PROVIDER_CANCELLED", "NO_ROSTER", "PROVIDER_SUSPENDED",
            "CUSTOMER_SUSPENDED"})
    @Tag("CHT-5")
    @DisplayName("Any cancellation before check-in ends the chat at once, whoever cancelled and whatever it cost: "
            + "Conversation reads only when it happened.")
    void theCauseOfACancellationDoesNotMatter(String cause) {
        bed.requestSaturdaysReservation();

        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", WEDNESDAY_9_00, cause));

        assertThat(windowStatus()).isEqualTo(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("The relay delivers the cancellation again: nothing changes.")
    void aRedeliveredCancellationChangesNothing() {
        bed.requestSaturdaysReservation();
        ReservationCancelledIE event = reservationCancelledEvent("cancelled-1", WEDNESDAY_9_00);
        bed.reservationCancelled.handle(event);
        int saves = bed.windows.saves();
        bed.windows.given(ReservationChatWindow.open(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME,
                PROTECTION_WINDOW_END));

        bed.reservationCancelled.handle(event);

        assertThat(bed.windows.saves()).isEqualTo(saves);
        assertThat(windowStatus()).isEqualTo(ChatWindowStatus.OPEN);
        assertThat(bed.processedMessages.hasHandled("cancelled-1", HandleReservationCancelledIE.HANDLER)).isTrue();
        assertThat(HandleReservationCancelledIE.HANDLER).isEqualTo("conversation.ReservationCancelled");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Out of order: the cancellation before check-in is heard before the request. The window is created "
            + "from it, with the Protection Window end it carries, and the request opens the chat read-only.")
    void aCancellationBeforeCheckInHeardBeforeTheRequestIsKept() {
        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", WEDNESDAY_9_00));

        ReservationChatWindow window = bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow();
        assertThat(window.status()).isEqualTo(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN);
        assertThat(window.customerId()).isEqualTo(AISYAH);
        assertThat(window.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(window.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        assertThat(bed.conversations.all()).isEmpty();

        bed.reservationRequested.handle(reservationRequestedEvent("requested-1"));

        assertThat(windowStatus()).isEqualTo(ChatWindowStatus.CANCELLED_BEFORE_CHECK_IN);
        bed.time.setNow(SATURDAY_9_40);
        assertThatThrownBy(() -> bed.sendMessage.send(
                new SendMessageRequest(bed.saturdaysChat().id().value(), AISYAH_ID, "Hi")))
                .isInstanceOf(ChatReadOnlyException.class);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Out of order: the cancellation after check-in is heard before the request. The window is created "
            + "open until Sunday 14:00, and the request opens the chat.")
    void aCancellationAfterCheckInHeardBeforeTheRequestIsKept() {
        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", SATURDAY_9_50));

        ReservationChatWindow window = bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow();
        assertThat(window.status()).isEqualTo(ChatWindowStatus.OPEN);
        assertThat(window.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        assertThat(bed.conversations.all()).isEmpty();

        bed.reservationRequested.handle(reservationRequestedEvent("requested-1"));

        assertThat(bed.saturdaysChat().messages()).isEmpty();
        assertThat(windowStatus()).isEqualTo(ChatWindowStatus.OPEN);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Only the reservation's chat window changes: no chat is saved by a cancellation.")
    void aCancellationOnlyChangesTheWindow() {
        bed.requestSaturdaysReservation();
        int chatSaves = bed.conversations.saves().size();

        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", WEDNESDAY_9_00));

        assertThat(bed.conversations.saves()).hasSize(chatSaves);
        assertThat(bed.windows.count()).isEqualTo(1);
    }
}
