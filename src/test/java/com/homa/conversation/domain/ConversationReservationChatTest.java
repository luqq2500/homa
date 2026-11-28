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
import static com.homa.conversation.domain.ConversationFixtures.saturdaysChat;
import static com.homa.conversation.domain.ConversationFixtures.windowOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.exception.ChatReadOnlyException;
import com.homa.conversation.domain.exception.EmptyMessageException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConversationReservationChatTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    @Test
    @Tag("CHT-1")
    @DisplayName("Aisyah's request for Saturday is Requested: the chat she starts from the reservation has her and "
            + "Kemas Home in it, and no messages yet.")
    void aReservationStartsItsChatBetweenTheCustomerAndTheProvider() {
        Conversation chat = Conversation.openReservationChat(requestedWindow(), REQUESTED_AT);

        assertThat(chat.kind()).isEqualTo(ConversationKind.RESERVATION);
        assertThat(chat.reservationId()).contains(SATURDAYS_RESERVATION);
        assertThat(chat.customerId()).isEqualTo(AISYAH);
        assertThat(chat.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(chat.startedAt()).isEqualTo(REQUESTED_AT);
        assertThat(chat.id().value()).isNotBlank();
        assertThat(chat.messages()).isEmpty();
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("Kemas Home messages \"We're at the guardhouse\" at 9:40. Aisyah messages about a missed corner on "
            + "Sunday morning.")
    void bothPartiesWriteInAReservationsChatFromRequestedUntilTheProtectionWindowEnds() {
        Conversation chat = saturdaysChat();
        Optional<ReservationChatWindow> window = windowOf(requestedWindow());

        Message guardhouse = chat.send(PartyRole.PROVIDER, "We're at the guardhouse", window, SATURDAY_9_40);
        Message missedCorner = chat.send(PartyRole.CUSTOMER, "You missed a corner of the kitchen", window,
                SUNDAY_MORNING);

        assertThat(guardhouse.author()).isEqualTo(PartyRole.PROVIDER);
        assertThat(guardhouse.sentAt()).isEqualTo(SATURDAY_9_40);
        assertThat(missedCorner.author()).isEqualTo(PartyRole.CUSTOMER);
        assertThat(chat.messages()).containsExactly(guardhouse, missedCorner);
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("The chat is open the moment Aisyah's reservation is Requested, and the provider may write first.")
    void eitherPartyCanWriteFirstOnceTheReservationIsRequested() {
        Conversation chat = saturdaysChat();

        Message hello = chat.send(PartyRole.PROVIDER, "Thank you for booking", windowOf(requestedWindow()),
                REQUESTED_AT);

        assertThat(chat.messages()).containsExactly(hello);
        assertThat(chat.accessAt(windowOf(requestedWindow()), REQUESTED_AT).isOpen()).isTrue();
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("One minute before the Protection Window ends at Sunday 14:00, a message is still accepted.")
    void aMessageOneMinuteBeforeTheProtectionWindowEndsIsAccepted() {
        Conversation chat = saturdaysChat();
        Instant lastMinute = PROTECTION_WINDOW_END.minus(ONE_MINUTE);

        Message message = chat.send(PartyRole.CUSTOMER, "Thanks again", windowOf(requestedWindow()), lastMinute);

        assertThat(message.sentAt()).isEqualTo(lastMinute);
        assertThat(chat.messages()).containsExactly(message);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("When the Protection Window ends at Sunday 14:00, a message is refused.")
    void aMessageWhenTheProtectionWindowEndsIsRefused() {
        Conversation chat = saturdaysChat();

        assertThatThrownBy(() -> chat.send(PartyRole.CUSTOMER, "Too late?", windowOf(requestedWindow()),
                PROTECTION_WINDOW_END))
                .isInstanceOfSatisfying(ChatReadOnlyException.class,
                        refusal -> assertThat(refusal.reason()).isEqualTo(ChatReadOnlyReason.PROTECTION_WINDOW_ENDED));
        assertThat(chat.messages()).isEmpty();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("One minute after the Protection Window ends, a message is refused.")
    void aMessageOneMinuteAfterTheProtectionWindowEndsIsRefused() {
        Conversation chat = saturdaysChat();

        assertThatThrownBy(() -> chat.send(PartyRole.PROVIDER, "One more thing", windowOf(requestedWindow()),
                PROTECTION_WINDOW_END.plus(ONE_MINUTE))).isInstanceOf(ChatReadOnlyException.class);
        assertThat(chat.messages()).isEmpty();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah can read Saturday's chat on Monday but not reply.")
    void theChatCanBeReadOnMondayButNotRepliedTo() {
        Conversation chat = saturdaysChat();
        Optional<ReservationChatWindow> window = windowOf(requestedWindow());
        chat.send(PartyRole.PROVIDER, "We're at the guardhouse", window, SATURDAY_9_40);

        assertThat(chat.messages()).extracting(Message::text).containsExactly("We're at the guardhouse");
        assertThat(chat.accessAt(window, MONDAY_9_00).readOnlyReason())
                .contains(ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);
        assertThatThrownBy(() -> chat.send(PartyRole.CUSTOMER, "Thanks!", window, MONDAY_9_00))
                .isInstanceOf(ChatReadOnlyException.class);
        assertThat(chat.messages()).hasSize(1);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Had Aisyah cancelled on Wednesday, the chat would be read-only from then.")
    void aChatIsReadOnlyFromTheMomentItsReservationIsCancelledBeforeCheckIn() {
        Conversation chat = saturdaysChat();
        Optional<ReservationChatWindow> cancelled = windowOf(requestedWindow().cancelled(WEDNESDAY_9_00,
                CHECK_IN_START));

        assertThatThrownBy(() -> chat.send(PartyRole.PROVIDER, "Sorry to see you go", cancelled, WEDNESDAY_9_00))
                .isInstanceOfSatisfying(ChatReadOnlyException.class, refusal -> assertThat(refusal.reason())
                        .isEqualTo(ChatReadOnlyReason.RESERVATION_CANCELLED_BEFORE_CHECK_IN));
        assertThat(chat.messages()).isEmpty();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah cancels at 9:50 on Saturday: the chat stays open until Sunday 14:00, so Kemas Home can "
            + "still tell her where the team is, and she can still reply the next morning.")
    void aChatStaysWritableUntilTheProtectionWindowEndsWhenItIsCancelledAfterCheckInStarted() {
        Conversation chat = saturdaysChat();
        Optional<ReservationChatWindow> cancelled = windowOf(requestedWindow().cancelled(SATURDAY_9_50,
                CHECK_IN_START));

        Message tellsHer = chat.send(PartyRole.PROVIDER, "The team is at the guardhouse", cancelled, SATURDAY_9_50);
        Message repliesNextMorning = chat.send(PartyRole.CUSTOMER, "I cancelled, sorry", cancelled, SUNDAY_MORNING);

        assertThat(chat.messages()).containsExactly(tellsHer, repliesNextMorning);
        assertThatThrownBy(() -> chat.send(PartyRole.CUSTOMER, "Hello?", cancelled, PROTECTION_WINDOW_END))
                .isInstanceOf(ChatReadOnlyException.class);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah withdraws Monday's request, or Kemas Home declines it, or it expires: nobody can write in "
            + "the chat from then on, and what was said stays readable.")
    void aChatIsReadOnlyAtOnceWhenItsReservationEndsBeforeCheckIn() {
        Conversation chat = saturdaysChat();
        chat.send(PartyRole.CUSTOMER, "Please bring a ladder", windowOf(requestedWindow()), REQUESTED_AT);

        for (ReservationChatWindow ended : new ReservationChatWindow[] {requestedWindow().withdrawn(),
                requestedWindow().declined(), requestedWindow().expired()}) {
            assertThatThrownBy(() -> chat.send(PartyRole.PROVIDER, "Hello?", Optional.of(ended), REQUESTED_AT))
                    .isInstanceOfSatisfying(ChatReadOnlyException.class, refusal -> assertThat(refusal.reason())
                            .isEqualTo(ended.status().readOnlyReason().orElseThrow()));
        }
        assertThat(chat.messages()).extracting(Message::text).containsExactly("Please bring a ladder");
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("A message with no words is not sent in an open chat.")
    void anEmptyMessageIsNotSentInAnOpenChat() {
        Conversation chat = saturdaysChat();

        assertThatThrownBy(() -> chat.send(PartyRole.CUSTOMER, "  ", windowOf(requestedWindow()), SATURDAY_9_40))
                .isInstanceOf(EmptyMessageException.class);
        assertThat(chat.messages()).isEmpty();
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("A reservation's chat says until when it is open.")
    void anOpenChatSaysWhenItTurnsReadOnly() {
        ChatAccess access = saturdaysChat().accessAt(windowOf(requestedWindow()), SATURDAY_9_40);

        assertThat(access.isOpen()).isTrue();
        assertThat(access.openUntil()).contains(PROTECTION_WINDOW_END);
    }

    @Test
    @DisplayName("A reservation's chat is judged by its own reservation's window, so it can't be asked without one "
            + "or with another reservation's.")
    void aReservationsChatNeedsTheWindowOfItsOwnReservation() {
        Conversation chat = saturdaysChat();
        ReservationChatWindow another = ReservationChatWindow.open(new ReservationId("reservation-sunday"), AISYAH,
                KEMAS_HOME, PROTECTION_WINDOW_END);

        assertThatThrownBy(() -> chat.accessAt(Optional.empty(), SATURDAY_9_40))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chat.accessAt(Optional.of(another), SATURDAY_9_40))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chat.send(PartyRole.CUSTOMER, "Hi", Optional.empty(), SATURDAY_9_40))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(chat.messages()).isEmpty();
    }
}
