package com.homa.conversation.usecase.ie;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.domain.ConversationFixtures.PROTECTION_WINDOW_END;
import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAYS_RESERVATION;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_40;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_50;
import static com.homa.conversation.domain.ConversationFixtures.WEDNESDAY_9_00;
import static com.homa.conversation.usecase.ConversationTestBed.AISYAH_ID;
import static com.homa.conversation.usecase.ConversationTestBed.AZMAN_PERSON_ID;
import static com.homa.conversation.usecase.ConversationTestBed.reservationCancelledEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationRequestedEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationWithdrawnEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.ChatWindowStatus;
import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.ConversationKind;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.exception.ChatReadOnlyException;
import com.homa.conversation.usecase.ConversationTestBed;
import com.homa.conversation.usecase.dto.SendMessageRequest;
import com.homa.ie.events.ReservationRequestedIE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationRequestedIETest {

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("CHT-1")
    @Tag("CHT-2")
    @DisplayName("Aisyah pays on Monday at 20:00 and her Saturday reservation is Requested: the chat between her and "
            + "Kemas Home opens, and stays open until Sunday 14:00.")
    void aRequestedReservationOpensItsChat() {
        bed.reservationRequested.handle(reservationRequestedEvent("requested-1"));

        Conversation chat = bed.saturdaysChat();
        assertThat(bed.conversations.all()).containsExactly(chat);
        assertThat(chat.kind()).isEqualTo(ConversationKind.RESERVATION);
        assertThat(chat.reservationId()).contains(SATURDAYS_RESERVATION);
        assertThat(chat.customerId()).isEqualTo(AISYAH);
        assertThat(chat.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(chat.startedAt()).isEqualTo(REQUESTED_AT);
        assertThat(chat.messages()).isEmpty();
        ReservationChatWindow window = bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow();
        assertThat(window.status()).isEqualTo(ChatWindowStatus.OPEN);
        assertThat(window.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        assertThat(window.customerId()).isEqualTo(AISYAH);
        assertThat(window.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(bed.conversations.saves()).containsExactly(chat.id());
        assertThat(bed.windows.saves()).isEqualTo(1);
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("Once the reservation is Requested, Kemas Home can write to Aisyah in its chat at once, and "
            + "Aisyah can write back.")
    void bothPartiesCanWriteOnceTheChatIsOpened() {
        bed.reservationRequested.handle(reservationRequestedEvent("requested-1"));
        String chatId = bed.saturdaysChat().id().value();
        bed.time.setNow(SATURDAY_9_40);

        assertThat(bed.sendMessage.send(new SendMessageRequest(chatId, AZMAN_PERSON_ID, "We're at the guardhouse"))
                .author()).isEqualTo("PROVIDER");
        assertThat(bed.sendMessage.send(new SendMessageRequest(chatId, AISYAH_ID, "Coming!")).author())
                .isEqualTo("CUSTOMER");
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("The relay delivers Aisyah's request again: nothing changes, and no second chat opens.")
    void aRedeliveredRequestChangesNothing() {
        ReservationRequestedIE event = reservationRequestedEvent("requested-1");
        bed.reservationRequested.handle(event);
        bed.windows.given(bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow().withdrawn());

        bed.reservationRequested.handle(event);

        assertThat(bed.conversations.all()).hasSize(1);
        assertThat(bed.conversations.saves()).hasSize(1);
        assertThat(bed.windows.saves()).isEqualTo(1);
        assertThat(bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow().status())
                .isEqualTo(ChatWindowStatus.WITHDRAWN);
        assertThat(bed.processedMessages.hasHandled("requested-1", HandleReservationRequestedIE.HANDLER)).isTrue();
        assertThat(HandleReservationRequestedIE.HANDLER).isEqualTo("conversation.ReservationRequested");
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("A reservation has one chat: another request event for the same reservation opens no second one.")
    void aReservationHasOneChat() {
        bed.reservationRequested.handle(reservationRequestedEvent("requested-1"));
        String chatId = bed.saturdaysChat().id().value();

        bed.reservationRequested.handle(reservationRequestedEvent("requested-2"));

        assertThat(bed.conversations.all()).hasSize(1);
        assertThat(bed.saturdaysChat().id().value()).isEqualTo(chatId);
        assertThat(bed.conversations.saves()).hasSize(1);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Out of order: the withdrawal is heard before the request. The chat still opens, read-only from the "
            + "start, and the Protection Window end is filled in; nothing is lost.")
    void aRequestHeardAfterItsWithdrawalOpensTheChatReadOnly() {
        bed.reservationWithdrawn.handle(reservationWithdrawnEvent("withdrawn-1", WEDNESDAY_9_00));

        bed.reservationRequested.handle(reservationRequestedEvent("requested-1"));

        ReservationChatWindow window = bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow();
        assertThat(window.status()).isEqualTo(ChatWindowStatus.WITHDRAWN);
        assertThat(window.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        Conversation chat = bed.saturdaysChat();
        assertThat(chat.startedAt()).isEqualTo(REQUESTED_AT);
        bed.time.setNow(SATURDAY_9_40);
        assertThatThrownBy(() -> bed.sendMessage.send(new SendMessageRequest(chat.id().value(), AISYAH_ID, "Hi")))
                .isInstanceOf(ChatReadOnlyException.class);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Out of order: Aisyah's cancellation at 9:50 on Saturday is heard before the request. The chat opens "
            + "and stays open until Sunday 14:00.")
    void aRequestHeardAfterACancellationAfterCheckInOpensAnOpenChat() {
        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", SATURDAY_9_50));

        bed.reservationRequested.handle(reservationRequestedEvent("requested-1"));

        ReservationChatWindow window = bed.windows.findByReservationId(SATURDAYS_RESERVATION).orElseThrow();
        assertThat(window.status()).isEqualTo(ChatWindowStatus.OPEN);
        assertThat(window.protectionWindowEnd()).contains(PROTECTION_WINDOW_END);
        bed.time.setNow(SATURDAY_9_50);
        assertThat(bed.sendMessage.send(new SendMessageRequest(bed.saturdaysChat().id().value(), AISYAH_ID, "Hi"))
                .text()).isEqualTo("Hi");
    }
}
