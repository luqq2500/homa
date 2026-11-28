package com.homa.conversation.usecase.ie;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.domain.ConversationFixtures.PROTECTION_WINDOW_END;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAYS_RESERVATION;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_40;
import static com.homa.conversation.domain.ConversationFixtures.WEDNESDAY_9_00;
import static com.homa.conversation.usecase.ConversationTestBed.AISYAH_ID;
import static com.homa.conversation.usecase.ConversationTestBed.AZMAN_PERSON_ID;
import static com.homa.conversation.usecase.ConversationTestBed.reservationRequestedEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationWithdrawnEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.ChatReadOnlyReason;
import com.homa.conversation.domain.ChatWindowStatus;
import com.homa.conversation.domain.Message;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.exception.ChatReadOnlyException;
import com.homa.conversation.usecase.ConversationTestBed;
import com.homa.conversation.usecase.dto.SendMessageRequest;
import com.homa.conversation.usecase.dto.ViewConversationRequest;
import com.homa.ie.events.ReservationWithdrawnIE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationWithdrawnIETest {

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah withdraws Monday's request on Wednesday: the chat is read-only from then, and what was said "
            + "stays readable.")
    void aWithdrawnReservationsChatIsReadOnlyAtOnce() {
        String chatId = bed.requestSaturdaysReservation();
        bed.time.setNow(WEDNESDAY_9_00);
        bed.sendMessage.send(new SendMessageRequest(chatId, AISYAH_ID, "Please bring a ladder"));

        bed.reservationWithdrawn.handle(reservationWithdrawnEvent("withdrawn-1", WEDNESDAY_9_00));

        ReservationChatWindow window = bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow();
        assertThat(window.status()).isEqualTo(ChatWindowStatus.WITHDRAWN);
        assertThat(window.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        assertThatThrownBy(() -> bed.sendMessage.send(new SendMessageRequest(chatId, AZMAN_PERSON_ID, "Sorry")))
                .isInstanceOfSatisfying(ChatReadOnlyException.class,
                        refusal -> assertThat(refusal.reason()).isEqualTo(ChatReadOnlyReason.RESERVATION_WITHDRAWN));
        assertThat(bed.viewConversation.view(new ViewConversationRequest(chatId, AISYAH_ID)).messages())
                .extracting(m -> m.text()).containsExactly("Please bring a ladder");
        assertThat(bed.saturdaysChat().messages()).extracting(Message::text).containsExactly("Please bring a ladder");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("The relay delivers the withdrawal again: nothing changes.")
    void aRedeliveredWithdrawalChangesNothing() {
        bed.requestSaturdaysReservation();
        ReservationWithdrawnIE event = reservationWithdrawnEvent("withdrawn-1", WEDNESDAY_9_00);
        bed.reservationWithdrawn.handle(event);
        int saves = bed.windows.saves();
        bed.windows.given(ReservationChatWindow.open(SATURDAYS_RESERVATION, AISYAH, KEMAS_HOME,
                PROTECTION_WINDOW_END));

        bed.reservationWithdrawn.handle(event);

        assertThat(bed.windows.saves()).isEqualTo(saves);
        assertThat(bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow().status())
                .isEqualTo(ChatWindowStatus.OPEN);
        assertThat(bed.processedMessages.hasHandled("withdrawn-1", HandleReservationWithdrawnIE.HANDLER)).isTrue();
        assertThat(HandleReservationWithdrawnIE.HANDLER).isEqualTo("conversation.ReservationWithdrawn");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Out of order: the withdrawal is heard before the request. It is kept, nothing crashes, and no chat "
            + "opens until the request arrives.")
    void aWithdrawalHeardBeforeTheRequestIsKept() {
        bed.reservationWithdrawn.handle(reservationWithdrawnEvent("withdrawn-1", WEDNESDAY_9_00));

        ReservationChatWindow window = bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow();
        assertThat(window.status()).isEqualTo(ChatWindowStatus.WITHDRAWN);
        assertThat(window.customerId()).isEqualTo(AISYAH);
        assertThat(window.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(window.protectionWindowEnd()).isEmpty();
        assertThat(bed.conversations.all()).isEmpty();

        bed.reservationRequested.handle(reservationRequestedEvent("requested-1"));

        assertThat(bed.saturdaysChat().messages()).isEmpty();
        assertThat(bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow().protectionWindowEnd())
                .contains(PROTECTION_WINDOW_END);
        bed.time.setNow(SATURDAY_9_40);
        assertThatThrownBy(() -> bed.sendMessage.send(
                new SendMessageRequest(bed.saturdaysChat().id().value(), AISYAH_ID, "Hi")))
                .isInstanceOf(ChatReadOnlyException.class);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Only the reservation's chat window changes: no chat is opened or saved by a withdrawal.")
    void aWithdrawalOnlyChangesTheWindow() {
        bed.requestSaturdaysReservation();
        int chatSaves = bed.conversations.saves().size();

        bed.reservationWithdrawn.handle(reservationWithdrawnEvent("withdrawn-1", WEDNESDAY_9_00));

        assertThat(bed.conversations.saves()).hasSize(chatSaves);
        assertThat(bed.conversations.all()).hasSize(1);
        assertThat(bed.windows.count()).isEqualTo(1);
    }
}
