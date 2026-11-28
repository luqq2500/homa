package com.homa.conversation.usecase;

import static com.homa.conversation.domain.ConversationFixtures.MONDAY_9_00;
import static com.homa.conversation.domain.ConversationFixtures.PROTECTION_WINDOW_END;
import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAY_9_40;
import static com.homa.conversation.domain.ConversationFixtures.WEDNESDAY_9_00;
import static com.homa.conversation.domain.ConversationFixtures.requestedWindow;
import static com.homa.conversation.usecase.ConversationTestBed.AISYAH_ID;
import static com.homa.conversation.usecase.ConversationTestBed.AZMAN_PERSON_ID;
import static com.homa.conversation.usecase.ConversationTestBed.KEMAS_HOME_ID;
import static com.homa.conversation.usecase.ConversationTestBed.SATURDAYS_RESERVATION_ID;
import static com.homa.conversation.usecase.ConversationTestBed.STRANGER_ID;
import static com.homa.conversation.usecase.ConversationTestBed.reservationCancelledEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationDeclinedEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationExpiredEvent;
import static com.homa.conversation.usecase.ConversationTestBed.reservationWithdrawnEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.Conversation;
import com.homa.conversation.usecase.dto.SendMessageRequest;
import com.homa.conversation.usecase.dto.StartEnquiryRequest;
import com.homa.conversation.usecase.dto.ViewConversationRequest;
import com.homa.conversation.usecase.dto.ViewConversationResponse;
import com.homa.conversation.usecase.dto.ViewConversationResponse.MessageView;
import com.homa.conversation.usecase.exception.ConversationNotFoundException;
import com.homa.conversation.usecase.exception.NotConversationPartyException;
import com.homa.conversation.usecase.exception.ProviderOwnerNotYetKnownException;
import com.homa.conversation.usecase.exception.ReservationChatWindowNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ViewConversationTest {

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();
    private final String chatId = bed.requestSaturdaysReservation();

    private ViewConversationResponse view(String viewerId) {
        return bed.viewConversation.view(new ViewConversationRequest(chatId, viewerId));
    }

    private void saturdayMessages() {
        bed.time.setNow(SATURDAY_9_40);
        bed.sendMessage.send(new SendMessageRequest(chatId, AZMAN_PERSON_ID, "We're at the guardhouse"));
    }

    @Test
    @Tag("CHT-2")
    @DisplayName("Kemas Home reads the chat on Saturday at 9:40: it is open until Sunday 14:00, and Azman is its "
            + "provider.")
    void theProviderReadsAnOpenChatAndSeesUntilWhenItIsWritable() {
        saturdayMessages();

        ViewConversationResponse chat = view(AZMAN_PERSON_ID);

        assertThat(chat.conversationId()).isEqualTo(chatId);
        assertThat(chat.kind()).isEqualTo("RESERVATION");
        assertThat(chat.customerId()).isEqualTo(AISYAH_ID);
        assertThat(chat.providerId()).isEqualTo(KEMAS_HOME_ID);
        assertThat(chat.reservationId()).isEqualTo(SATURDAYS_RESERVATION_ID);
        assertThat(chat.yourRole()).isEqualTo("PROVIDER");
        assertThat(chat.writable()).isTrue();
        assertThat(chat.readOnlyReason()).isNull();
        assertThat(chat.writableUntil()).isEqualTo(PROTECTION_WINDOW_END);
        assertThat(chat.messages()).containsExactly(new MessageView(
                bed.saturdaysChat().messages().getFirst().id().value(), "PROVIDER", "We're at the guardhouse",
                SATURDAY_9_40));
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Aisyah can read Saturday's chat on Monday but not reply.")
    void theCustomerReadsAChatWhoseProtectionWindowEndedButCannotReply() {
        saturdayMessages();
        bed.time.setNow(MONDAY_9_00);

        ViewConversationResponse chat = view(AISYAH_ID);

        assertThat(chat.yourRole()).isEqualTo("CUSTOMER");
        assertThat(chat.writable()).isFalse();
        assertThat(chat.readOnlyReason()).isEqualTo("PROTECTION_WINDOW_ENDED");
        assertThat(chat.writableUntil()).isNull();
        assertThat(chat.messages()).extracting(MessageView::text).containsExactly("We're at the guardhouse");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("Had Aisyah cancelled on Wednesday, she would read the chat as read-only from then.")
    void aChatCancelledBeforeCheckInIsViewedAsReadOnly() {
        bed.reservationCancelled.handle(reservationCancelledEvent("cancelled-1", WEDNESDAY_9_00));
        bed.time.setNow(WEDNESDAY_9_00);

        ViewConversationResponse chat = view(AISYAH_ID);

        assertThat(chat.writable()).isFalse();
        assertThat(chat.readOnlyReason()).isEqualTo("RESERVATION_CANCELLED_BEFORE_CHECK_IN");
        assertThat(chat.writableUntil()).isNull();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("A Withdrawn, Declined or Expired reservation's chat is viewed as read-only, with the reason.")
    void aChatEndedBeforeCheckInIsViewedAsReadOnlyWithItsReason() {
        ConversationTestBed withdrawn = ConversationTestBed.withKemasHomeAndAisyah();
        String withdrawnChat = withdrawn.requestSaturdaysReservation();
        withdrawn.reservationWithdrawn.handle(reservationWithdrawnEvent("ended-1", SATURDAY_9_40));
        ConversationTestBed declined = ConversationTestBed.withKemasHomeAndAisyah();
        String declinedChat = declined.requestSaturdaysReservation();
        declined.reservationDeclined.handle(reservationDeclinedEvent("ended-1", SATURDAY_9_40));
        ConversationTestBed expired = ConversationTestBed.withKemasHomeAndAisyah();
        String expiredChat = expired.requestSaturdaysReservation();
        expired.reservationExpired.handle(reservationExpiredEvent("ended-1", SATURDAY_9_40));

        assertThat(withdrawn.viewConversation.view(new ViewConversationRequest(withdrawnChat, AISYAH_ID))
                .readOnlyReason()).isEqualTo("RESERVATION_WITHDRAWN");
        assertThat(declined.viewConversation.view(new ViewConversationRequest(declinedChat, AISYAH_ID))
                .readOnlyReason()).isEqualTo("RESERVATION_DECLINED");
        assertThat(expired.viewConversation.view(new ViewConversationRequest(expiredChat, AISYAH_ID))
                .readOnlyReason()).isEqualTo("RESERVATION_EXPIRED");
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Aisyah reads her Enquiry with Kemas Home: it names no reservation, and no Protection Window ends "
            + "it.")
    void anEnquiryIsViewedAsAlwaysWritable() {
        String enquiryId = bed.startEnquiry.start(new StartEnquiryRequest(AISYAH_ID, KEMAS_HOME_ID,
                "Do you bring a ladder?")).conversationId();
        bed.time.setNow(MONDAY_9_00);

        ViewConversationResponse enquiry = bed.viewConversation.view(new ViewConversationRequest(enquiryId,
                AISYAH_ID));

        assertThat(enquiry.kind()).isEqualTo("ENQUIRY");
        assertThat(enquiry.reservationId()).isNull();
        assertThat(enquiry.writable()).isTrue();
        assertThat(enquiry.readOnlyReason()).isNull();
        assertThat(enquiry.writableUntil()).isNull();
        assertThat(enquiry.messages()).extracting(MessageView::text).containsExactly("Do you bring a ladder?");
        assertThat(enquiry.messages().getFirst().author()).isEqualTo("CUSTOMER");
        assertThat(enquiry.messages().getFirst().sentAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    @Tag("CHT-3")
    @DisplayName("Kemas Home offers a cash discount: both parties read it as written. The reminder that a cash job "
            + "wouldn't be covered is shown by the apps on every chat, so it isn't in what Conversation returns.")
    void aCashDiscountOfferIsReadAsWritten() {
        bed.time.setNow(SATURDAY_9_40);
        bed.sendMessage.send(new SendMessageRequest(chatId, AZMAN_PERSON_ID, "Pay cash and I'll take RM20 off"));

        assertThat(view(AISYAH_ID).messages()).extracting(MessageView::text)
                .containsExactly("Pay cash and I'll take RM20 off");
        assertThat(view(AZMAN_PERSON_ID).messages()).extracting(MessageView::text)
                .containsExactly("Pay cash and I'll take RM20 off");
    }

    @Test
    @DisplayName("Someone who is neither Aisyah nor Kemas Home's owner can't read the chat. Support's reading as "
            + "evidence (CHT-4) is slice 5.")
    void aStrangerCannotReadAChat() {
        assertThatThrownBy(() -> view(STRANGER_ID)).isInstanceOf(NotConversationPartyException.class)
                .hasMessageContaining(STRANGER_ID);
    }

    @Test
    @DisplayName("No conversation, no view.")
    void aMissingConversationCannotBeRead() {
        assertThatThrownBy(() -> bed.viewConversation.view(new ViewConversationRequest("conversation-none",
                AISYAH_ID))).isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    @DisplayName("While Conversation doesn't know Kemas Home's owner yet, the provider's side can't be read: retry "
            + "later.")
    void theProviderSideCannotBeReadBeforeItsOwnerIsKnown() {
        ViewConversation withoutOwner = new ViewConversation(bed.conversations, bed.windows,
                new InMemoryPorts.Owners(), bed.time);

        assertThatThrownBy(() -> withoutOwner.view(new ViewConversationRequest(chatId, AZMAN_PERSON_ID)))
                .isInstanceOf(ProviderOwnerNotYetKnownException.class);
        assertThat(withoutOwner.view(new ViewConversationRequest(chatId, AISYAH_ID)).yourRole())
                .isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("A reservation's chat whose window is missing from storage is reported, not guessed at.")
    void aReservationChatWithoutItsWindowIsReported() {
        ConversationTestBed incomplete = ConversationTestBed.withKemasHomeAndAisyah();
        Conversation chat = incomplete.conversations.given(
                Conversation.openReservationChat(requestedWindow(), REQUESTED_AT));

        assertThatThrownBy(() -> incomplete.viewConversation.view(
                new ViewConversationRequest(chat.id().value(), AISYAH_ID)))
                .isInstanceOf(ReservationChatWindowNotFoundException.class);
    }

    @Test
    @DisplayName("The messages in a view are a copy: changing the response doesn't change the chat.")
    void aViewHoldsACopyOfTheMessages() {
        saturdayMessages();

        ViewConversationResponse chat = view(AISYAH_ID);

        assertThatThrownBy(() -> chat.messages().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(bed.saturdaysChat().messages()).hasSize(1);
    }
}
