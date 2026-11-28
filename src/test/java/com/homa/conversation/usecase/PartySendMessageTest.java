package com.homa.conversation.usecase;

import static com.homa.conversation.domain.ConversationFixtures.DANIEL;
import static com.homa.conversation.domain.ConversationFixtures.CHECK_IN_START;
import static com.homa.conversation.domain.ConversationFixtures.MONDAY_9_00;
import static com.homa.conversation.domain.ConversationFixtures.PROTECTION_WINDOW_END;
import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_40;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_50;
import static com.homa.conversation.domain.ConversationFixtures.SUNDAY_MORNING;
import static com.homa.conversation.domain.ConversationFixtures.WEDNESDAY_9_00;
import static com.homa.conversation.domain.ConversationFixtures.requestedWindow;
import static com.homa.conversation.usecase.ConversationTestBed.AISYAH_ID;
import static com.homa.conversation.usecase.ConversationTestBed.AZMAN_PERSON_ID;
import static com.homa.conversation.usecase.ConversationTestBed.KEMAS_HOME_ID;
import static com.homa.conversation.usecase.ConversationTestBed.STRANGER_ID;
import static com.homa.conversation.usecase.ConversationTestBed.reservationCancelledEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationDeclinedEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationExpiredEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationWithdrawnEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.ChatReadOnlyReason;
import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.ConversationId;
import com.homa.conversation.domain.Message;
import com.homa.conversation.domain.PartyRole;
import com.homa.conversation.domain.exception.ChatReadOnlyException;
import com.homa.conversation.domain.exception.EmptyMessageException;
import com.homa.conversation.usecase.dto.SendMessageRequest;
import com.homa.conversation.usecase.dto.SendMessageResponse;
import com.homa.conversation.usecase.dto.StartEnquiryRequest;
import com.homa.conversation.usecase.dto.ViewConversationRequest;
import com.homa.conversation.usecase.exception.ConversationNotFoundException;
import com.homa.conversation.usecase.exception.NotConversationPartyException;
import com.homa.conversation.usecase.exception.ProviderOwnerNotYetKnownException;
import com.homa.conversation.usecase.exception.ReservationChatWindowNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PartySendMessageTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();
    private final String chatId = bed.requestSaturdaysReservation();

    private SendMessageResponse send(String senderId, String text, Instant at) {
        bed.time.setNow(at);
        return bed.sendMessage.send(new SendMessageRequest(chatId, senderId, text));
    }

    private void assertRefused(String senderId, Instant at, ChatReadOnlyReason reason) {
        int messages = bed.saturdaysChat().messages().size();
        assertThatThrownBy(() -> send(senderId, "Hello?", at))
                .isInstanceOfSatisfying(ChatReadOnlyException.class,
                        refusal -> assertThat(refusal.reason()).isEqualTo(reason));
        assertThat(bed.saturdaysChat().messages()).hasSize(messages);
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("Kemas Home messages \"We're at the guardhouse\" at 9:40. Aisyah messages about a missed corner on "
            + "Sunday morning.")
    void bothPartiesWriteInAReservationsChatUntilTheProtectionWindowEnds() {
        SendMessageResponse guardhouse = send(AZMAN_PERSON_ID, "We're at the guardhouse", SATURDAY_9_40);
        SendMessageResponse missedCorner = send(AISYAH_ID, "You missed a corner of the kitchen", SUNDAY_MORNING);

        Conversation chat = bed.saturdaysChat();
        assertThat(chat.messages()).extracting(Message::text)
                .containsExactly("We're at the guardhouse", "You missed a corner of the kitchen");
        assertThat(chat.messages()).extracting(Message::author).containsExactly(PartyRole.PROVIDER, PartyRole.CUSTOMER);
        assertThat(guardhouse.conversationId()).isEqualTo(chatId);
        assertThat(guardhouse.messageId()).isEqualTo(chat.messages().get(0).id().value());
        assertThat(guardhouse.author()).isEqualTo("PROVIDER");
        assertThat(guardhouse.text()).isEqualTo("We're at the guardhouse");
        assertThat(guardhouse.sentAt()).isEqualTo(SATURDAY_9_40);
        assertThat(missedCorner.author()).isEqualTo("CUSTOMER");
        assertThat(missedCorner.sentAt()).isEqualTo(SUNDAY_MORNING);
        assertThat(bed.conversations.saves()).containsExactly(chat.id(), chat.id(), chat.id());  // opened, then two messages
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("A message sent one minute before the Protection Window ends at Sunday 14:00 is accepted.")
    void aMessageOneMinuteBeforeTheProtectionWindowEndsIsAccepted() {
        SendMessageResponse sent = send(AISYAH_ID, "Thanks again", PROTECTION_WINDOW_END.minus(ONE_MINUTE));

        assertThat(sent.sentAt()).isEqualTo(PROTECTION_WINDOW_END.minus(ONE_MINUTE));
        assertThat(bed.saturdaysChat().messages()).hasSize(1);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A message sent when the Protection Window ends at Sunday 14:00, or one minute after, is refused.")
    void aMessageFromTheMomentTheProtectionWindowEndsIsRefused() {
        assertRefused(AISYAH_ID, PROTECTION_WINDOW_END, ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);
        assertRefused(AZMAN_PERSON_ID, PROTECTION_WINDOW_END.plus(ONE_MINUTE),
                ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah can read Saturday's chat on Monday but not reply.")
    void aChatCanBeReadButNotRepliedToAfterTheProtectionWindowEnded() {
        send(AZMAN_PERSON_ID, "We're at the guardhouse", SATURDAY_9_40);

        assertRefused(AISYAH_ID, MONDAY_9_00, ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);

        bed.time.setNow(MONDAY_9_00);
        var view = bed.viewConversation.view(new ViewConversationRequest(chatId, AISYAH_ID));
        assertThat(view.writable()).isFalse();
        assertThat(view.messages()).extracting(m -> m.text()).containsExactly("We're at the guardhouse");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah cancels on Wednesday, before the Check-in Period: the chat is read-only from then.")
    void aChatIsReadOnlyFromTheMomentItsReservationIsCancelledBeforeCheckIn() {
        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", WEDNESDAY_9_00));

        assertRefused(AISYAH_ID, WEDNESDAY_9_00, ChatReadOnlyReason.RESERVATION_CANCELLED_BEFORE_CHECK_IN);
        assertRefused(AZMAN_PERSON_ID, SATURDAY_9_40, ChatReadOnlyReason.RESERVATION_CANCELLED_BEFORE_CHECK_IN);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah cancels at 9:50 on Saturday: the chat stays open until Sunday 14:00.")
    void aChatStaysWritableUntilTheProtectionWindowEndsWhenItIsCancelledAfterCheckInStarted() {
        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", SATURDAY_9_50));

        send(AZMAN_PERSON_ID, "We're sorry you cancelled", SATURDAY_9_50);
        send(AISYAH_ID, "I'm sorry too", SUNDAY_MORNING);

        assertThat(bed.saturdaysChat().messages()).hasSize(2);
        assertRefused(AISYAH_ID, PROTECTION_WINDOW_END, ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A cancellation one minute before the Check-in Period starts at 9:45 ends the chat; one minute after, "
            + "it stays open.")
    void theCheckInPeriodStartDecidesWhetherACancellationEndsTheChat() {
        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", CHECK_IN_START.minus(ONE_MINUTE)));
        assertRefused(AISYAH_ID, SATURDAY_9_50, ChatReadOnlyReason.RESERVATION_CANCELLED_BEFORE_CHECK_IN);

        ConversationTestBed later = ConversationTestBed.withKemasHomeAndAisyah();
        String laterChat = later.requestSaturdaysReservation();
        later.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", CHECK_IN_START.plus(ONE_MINUTE)));
        later.time.setNow(SATURDAY_9_50);
        assertThat(later.sendMessage.send(new SendMessageRequest(laterChat, AISYAH_ID, "Still here")).text())
                .isEqualTo("Still here");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah's reservation is Withdrawn, Declined or Expired: its chat is read-only at once.")
    void aChatIsReadOnlyAtOnceWhenItsReservationEndsBeforeCheckIn() {
        for (Consumer<ConversationTestBed> ending : List.<Consumer<ConversationTestBed>>of(
                b -> b.reservationWithdrawn.handle(reservationWithdrawnEvent("ended-1", SATURDAY_9_40)),
                b -> b.reservationDeclined.handle(reservationDeclinedEvent("ended-1", SATURDAY_9_40)),
                b -> b.reservationExpired.handle(reservationExpiredEvent("ended-1", SATURDAY_9_40)))) {
            ConversationTestBed ended = ConversationTestBed.withKemasHomeAndAisyah();
            String endedChat = ended.requestSaturdaysReservation();
            ending.accept(ended);
            ended.time.setNow(SATURDAY_9_40);

            assertThatThrownBy(() -> ended.sendMessage.send(new SendMessageRequest(endedChat, AZMAN_PERSON_ID, "Hi")))
                    .isInstanceOf(ChatReadOnlyException.class);
            assertThat(ended.saturdaysChat().messages()).isEmpty();
        }
    }

    @Test
    @Tag("CHT-3")
    @DisplayName("Kemas Home offers a cash discount: Conversation delivers the message as written. The reminder that "
            + "a cash job wouldn't be covered is the apps' copy on every chat, not something the chat adds or blocks.")
    void aCashDiscountOfferIsDeliveredAsWritten() {
        String offer = "Pay cash on the day and I'll take RM20 off!";

        SendMessageResponse sent = send(AZMAN_PERSON_ID, offer, SATURDAY_9_40);

        assertThat(sent.text()).isEqualTo(offer);
        assertThat(bed.saturdaysChat().messages()).extracting(Message::text).containsExactly(offer);
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Kemas Home answers Aisyah's question about a ladder in the Enquiry she started, and she replies.")
    void theProviderAnswersAnEnquiryTheCustomerStarted() {
        String enquiryId = bed.startEnquiry.start(new StartEnquiryRequest(AISYAH_ID, KEMAS_HOME_ID,
                "Do you bring a ladder?")).conversationId();
        bed.time.setNow(REQUESTED_AT.plus(Duration.ofMinutes(10)));

        SendMessageResponse answer = bed.sendMessage.send(new SendMessageRequest(enquiryId, AZMAN_PERSON_ID,
                "Yes, a 2-metre ladder."));
        SendMessageResponse reply = bed.sendMessage.send(new SendMessageRequest(enquiryId, AISYAH_ID, "Thank you!"));

        assertThat(answer.author()).isEqualTo("PROVIDER");
        assertThat(reply.author()).isEqualTo("CUSTOMER");
        assertThat(bed.conversations.findById(new ConversationId(enquiryId)).orElseThrow().messages())
                .extracting(Message::text).containsExactly("Do you bring a ladder?", "Yes, a 2-metre ladder.",
                        "Thank you!");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("An Enquiry stays open long after a reservation's chat has turned read-only: Aisyah's new question "
            + "goes in a new Enquiry, and Kemas Home can answer it.")
    void anEnquiryIsWritableWhenTheReservationsChatIsNot() {
        assertRefused(AISYAH_ID, MONDAY_9_00, ChatReadOnlyReason.PROTECTION_WINDOW_ENDED);
        bed.time.setNow(MONDAY_9_00);
        String enquiryId = bed.startEnquiry.start(new StartEnquiryRequest(AISYAH_ID, KEMAS_HOME_ID,
                "The corner I mentioned: can you come back to fix it?")).conversationId();

        SendMessageResponse answer = bed.sendMessage.send(new SendMessageRequest(enquiryId, AZMAN_PERSON_ID,
                "Yes, on Wednesday."));

        assertThat(answer.text()).isEqualTo("Yes, on Wednesday.");
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Kemas Home can't message customers who haven't contacted it: there is no chat for Azman to write in "
            + "until Aisyah starts one.")
    void aProviderHasNoChatToWriteInUntilTheCustomerStartsOne() {
        ConversationTestBed quiet = ConversationTestBed.withKemasHomeAndAisyah();

        assertThatThrownBy(() -> quiet.sendMessage.send(new SendMessageRequest("conversation-none", AZMAN_PERSON_ID,
                "Want a discount?"))).isInstanceOf(ConversationNotFoundException.class)
                .hasMessageContaining("conversation-none");
        assertThat(quiet.conversations.all()).isEmpty();
    }

    @Test
    @DisplayName("Someone who is neither Aisyah nor Kemas Home's owner can't write in the chat, and nothing is saved.")
    void aStrangerCannotWriteInAChat() {
        int saves = bed.conversations.saves().size();

        assertThatThrownBy(() -> send(STRANGER_ID, "Hello", SATURDAY_9_40))
                .isInstanceOf(NotConversationPartyException.class)
                .hasMessageContaining(STRANGER_ID);
        assertThatThrownBy(() -> send(DANIEL.value(), "Hello", SATURDAY_9_40))
                .isInstanceOf(NotConversationPartyException.class);
        assertThat(bed.conversations.saves()).hasSize(saves);
        assertThat(bed.saturdaysChat().messages()).isEmpty();
    }

    @Test
    @DisplayName("While Conversation doesn't know Kemas Home's owner yet, nobody but the customer can write for the "
            + "provider: the call is refused to be retried, not handed to whoever asks.")
    void theProviderCannotBeSpokenForBeforeItsOwnerIsKnown() {
        ConversationTestBed noOwner = ConversationTestBed.withKemasHomeAndAisyah();
        String chat = noOwner.requestSaturdaysReservation();
        PartySendMessage withoutOwner = new PartySendMessage(noOwner.conversations, noOwner.windows,
                new InMemoryPorts.Owners(), noOwner.time);
        noOwner.time.setNow(SATURDAY_9_40);

        assertThatThrownBy(() -> withoutOwner.send(new SendMessageRequest(chat, AZMAN_PERSON_ID, "Hello")))
                .isInstanceOf(ProviderOwnerNotYetKnownException.class)
                .hasMessageContaining(KEMAS_HOME_ID);
        assertThat(withoutOwner.send(new SendMessageRequest(chat, AISYAH_ID, "Hello")).author())
                .isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("A message with no words is not sent, and nothing is saved.")
    void anEmptyMessageIsNotSent() {
        int saves = bed.conversations.saves().size();

        assertThatThrownBy(() -> send(AISYAH_ID, " ", SATURDAY_9_40)).isInstanceOf(EmptyMessageException.class);

        assertThat(bed.conversations.saves()).hasSize(saves);
    }

    @Test
    @DisplayName("A reservation's chat whose window is missing from storage is reported, not guessed at.")
    void aReservationChatWithoutItsWindowIsReported() {
        ConversationTestBed incomplete = ConversationTestBed.withKemasHomeAndAisyah();
        Conversation chat = incomplete.conversations.given(
                Conversation.openReservationChat(requestedWindow(), REQUESTED_AT));

        assertThatThrownBy(() -> incomplete.sendMessage.send(
                new SendMessageRequest(chat.id().value(), AISYAH_ID, "Hello")))
                .isInstanceOf(ReservationChatWindowNotFoundException.class)
                .hasMessageContaining(ConversationTestBed.SATURDAYS_RESERVATION_ID);
    }
}
