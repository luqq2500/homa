package com.homa.conversation.usecase.ie;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.domain.ConversationFixtures.PROTECTION_WINDOW_END;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAYS_RESERVATION;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_40;
import static com.homa.conversation.domain.ConversationFixtures.WEDNESDAY_9_00;
import static com.homa.conversation.usecase.ConversationTestBed.AISYAH_ID;
import static com.homa.conversation.usecase.ConversationTestBed.AZMAN_PERSON_ID;
import static com.homa.conversation.usecase.ConversationTestBed.reservationDeclinedEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationRequestedEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationWithdrawnEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.ChatReadOnlyReason;
import com.homa.conversation.domain.ChatWindowStatus;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.exception.ChatReadOnlyException;
import com.homa.conversation.usecase.ConversationTestBed;
import com.homa.conversation.usecase.dto.SendMessageRequest;
import com.homa.ie.events.ReservationDeclinedIE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationDeclinedIETest {

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("CHT-5")
    @DisplayName("Kemas Home declines because two helpers are on leave: the chat is read-only at once, though the "
            + "Check-in Period is days away.")
    void aDeclinedReservationsChatIsReadOnlyAtOnce() {
        String chatId = bed.requestSaturdaysReservation();
        bed.time.setNow(WEDNESDAY_9_00);

        bed.reservationDeclined.handle(reservationDeclinedEvent("declined-1", WEDNESDAY_9_00));

        ReservationChatWindow window = bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow();
        assertThat(window.status()).isEqualTo(ChatWindowStatus.DECLINED);
        assertThat(window.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        assertThatThrownBy(() -> bed.sendMessage.send(new SendMessageRequest(chatId, AISYAH_ID, "Why?")))
                .isInstanceOfSatisfying(ChatReadOnlyException.class,
                        refusal -> assertThat(refusal.reason()).isEqualTo(ChatReadOnlyReason.RESERVATION_DECLINED));
        assertThatThrownBy(() -> bed.sendMessage.send(new SendMessageRequest(chatId, AZMAN_PERSON_ID, "Sorry")))
                .isInstanceOf(ChatReadOnlyException.class);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("The relay delivers the decline again: nothing changes.")
    void aRedeliveredDeclineChangesNothing() {
        bed.requestSaturdaysReservation();
        ReservationDeclinedIE event = reservationDeclinedEvent("declined-1", WEDNESDAY_9_00);
        bed.reservationDeclined.handle(event);
        int saves = bed.windows.saves();
        bed.windows.given(ReservationChatWindow.open(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME,
                PROTECTION_WINDOW_END));

        bed.reservationDeclined.handle(event);

        assertThat(bed.windows.saves()).isEqualTo(saves);
        assertThat(bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow().status())
                .isEqualTo(ChatWindowStatus.OPEN);
        assertThat(bed.processedMessages.hasHandled("declined-1", HandleReservationDeclinedIE.HANDLER)).isTrue();
        assertThat(HandleReservationDeclinedIE.HANDLER).isEqualTo("conversation.ReservationDeclined");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Out of order: the decline is heard before the request. It is kept, nothing crashes, and the request "
            + "later opens the chat read-only.")
    void aDeclineHeardBeforeTheRequestIsKept() {
        bed.reservationDeclined.handle(reservationDeclinedEvent("declined-1", WEDNESDAY_9_00));

        ReservationChatWindow window = bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow();
        assertThat(window.status()).isEqualTo(ChatWindowStatus.DECLINED);
        assertThat(window.customerId()).isEqualTo(AISYAH);
        assertThat(window.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(window.protectionWindowEnd()).isEmpty();
        assertThat(bed.conversations.all()).isEmpty();

        bed.reservationRequested.handle(reservationRequestedEvent("requested-1"));

        assertThat(bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow().status())
                .isEqualTo(ChatWindowStatus.DECLINED);
        bed.time.setNow(SATURDAY_9_40);
        assertThatThrownBy(() -> bed.sendMessage.send(
                new SendMessageRequest(bed.saturdaysChat().id().value(), AISYAH_ID, "Hi")))
                .isInstanceOf(ChatReadOnlyException.class);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A reservation that has already ended another way keeps its first ending: Aisyah's withdrawal "
            + "stands when a decline is heard after it.")
    void aLaterEndingDoesNotChangeTheFirst() {
        bed.requestSaturdaysReservation();
        bed.reservationWithdrawn.handle(reservationWithdrawnEvent("withdrawn-1", WEDNESDAY_9_00));

        bed.reservationDeclined.handle(reservationDeclinedEvent("declined-1", WEDNESDAY_9_00));

        assertThat(bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow().status())
                .isEqualTo(ChatWindowStatus.WITHDRAWN);
    }
}
