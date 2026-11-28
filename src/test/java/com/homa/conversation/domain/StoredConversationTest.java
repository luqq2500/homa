package com.homa.conversation.domain;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;
import static com.homa.conversation.domain.ConversationFixtures.SATURDAYS_RESERVATION;
import static com.homa.conversation.domain.ConversationFixtures.aisyahsEnquiry;
import static com.homa.conversation.domain.ConversationFixtures.saturdaysChat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredConversationTest {

    private static final Message CUSTOMERS_QUESTION = new Message(new MessageId("message-1"), PartyRole.CUSTOMER,
            "Do you bring a ladder?", REQUESTED_AT);
    private static final Message PROVIDERS_ANSWER = new Message(new MessageId("message-2"), PartyRole.PROVIDER,
            "Yes.", REQUESTED_AT.plus(Duration.ofMinutes(5)));

    @Test
    @DisplayName("A stored Enquiry is rebuilt with its messages in the order they were sent.")
    void aStoredEnquiryIsRebuiltAsItWas() {
        Conversation enquiry = Conversation.restore(new ConversationId("conversation-1"), ConversationKind.ENQUIRY,
                AISYAH, KEMAS_HOME, null, REQUESTED_AT, List.of(CUSTOMERS_QUESTION, PROVIDERS_ANSWER));

        assertThat(enquiry.id()).isEqualTo(new ConversationId("conversation-1"));
        assertThat(enquiry.kind()).isEqualTo(ConversationKind.ENQUIRY);
        assertThat(enquiry.customerId()).isEqualTo(AISYAH);
        assertThat(enquiry.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(enquiry.reservationId()).isEmpty();
        assertThat(enquiry.startedAt()).isEqualTo(REQUESTED_AT);
        assertThat(enquiry.messages()).containsExactly(CUSTOMERS_QUESTION, PROVIDERS_ANSWER);
    }

    @Test
    @DisplayName("A stored reservation's chat is rebuilt with its reservation, and may have no messages yet.")
    void aStoredReservationChatIsRebuiltAsItWas() {
        Conversation chat = Conversation.restore(new ConversationId("conversation-2"), ConversationKind.RESERVATION,
                AISYAH, KEMAS_HOME, SATURDAYS_RESERVATION, REQUESTED_AT, List.of(PROVIDERS_ANSWER));

        assertThat(chat.kind()).isEqualTo(ConversationKind.RESERVATION);
        assertThat(chat.reservationId()).contains(SATURDAYS_RESERVATION);
        assertThat(chat.messages()).containsExactly(PROVIDERS_ANSWER);
        assertThat(Conversation.restore(chat.id(), ConversationKind.RESERVATION, AISYAH, KEMAS_HOME,
                SATURDAYS_RESERVATION, REQUESTED_AT, List.of()).messages()).isEmpty();
    }

    @Test
    @DisplayName("A rebuilt conversation goes on from where it was: the next message follows the stored ones.")
    void aRebuiltConversationGoesOn() {
        Conversation enquiry = Conversation.restore(new ConversationId("conversation-1"), ConversationKind.ENQUIRY,
                AISYAH, KEMAS_HOME, null, REQUESTED_AT, new ArrayList<>(List.of(CUSTOMERS_QUESTION)));

        Message next = enquiry.send(PartyRole.PROVIDER, "Yes.", Optional.empty(), REQUESTED_AT.plusSeconds(60));

        assertThat(enquiry.messages()).containsExactly(CUSTOMERS_QUESTION, next);
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Kemas Home can't message customers who haven't contacted it: a stored Enquiry that begins with "
            + "the provider's message, or with none, is refused.")
    void aStoredEnquiryMustBeginWithItsCustomersMessage() {
        assertThatThrownBy(() -> Conversation.restore(new ConversationId("conversation-1"), ConversationKind.ENQUIRY,
                AISYAH, KEMAS_HOME, null, REQUESTED_AT, List.of(PROVIDERS_ANSWER, CUSTOMERS_QUESTION)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("begins with its customer");
        assertThatThrownBy(() -> Conversation.restore(new ConversationId("conversation-1"), ConversationKind.ENQUIRY,
                AISYAH, KEMAS_HOME, null, REQUESTED_AT, List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("begins with its customer");
    }

    @Test
    @DisplayName("A stored reservation's chat names its reservation, and a stored Enquiry doesn't.")
    void aStoredConversationNamesItsReservationExactlyWhenItIsAReservationsChat() {
        assertThatThrownBy(() -> Conversation.restore(new ConversationId("conversation-2"),
                ConversationKind.RESERVATION, AISYAH, KEMAS_HOME, null, REQUESTED_AT, List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reservation exactly when");
        assertThatThrownBy(() -> Conversation.restore(new ConversationId("conversation-1"), ConversationKind.ENQUIRY,
                AISYAH, KEMAS_HOME, SATURDAYS_RESERVATION, REQUESTED_AT, List.of(CUSTOMERS_QUESTION)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reservation exactly when");
    }

    @Test
    @DisplayName("A conversation freshly started can be stored and rebuilt unchanged.")
    void aConversationSurvivesBeingStoredAndRebuilt() {
        Conversation enquiry = aisyahsEnquiry();
        Conversation chat = saturdaysChat();

        Conversation rebuiltEnquiry = Conversation.restore(enquiry.id(), enquiry.kind(), enquiry.customerId(),
                enquiry.providerId(), enquiry.reservationId().orElse(null), enquiry.startedAt(), enquiry.messages());
        Conversation rebuiltChat = Conversation.restore(chat.id(), chat.kind(), chat.customerId(),
                chat.providerId(), chat.reservationId().orElse(null), chat.startedAt(), chat.messages());

        assertThat(rebuiltEnquiry.messages()).isEqualTo(enquiry.messages());
        assertThat(rebuiltChat.reservationId()).isEqualTo(chat.reservationId());
    }

    @Test
    @DisplayName("A stored conversation needs its id, kind, parties and start.")
    void aStoredConversationNeedsItsIdentityPartiesAndStart() {
        ConversationId id = new ConversationId("conversation-1");
        List<Message> question = List.of(CUSTOMERS_QUESTION);

        assertThatThrownBy(() -> Conversation.restore(null, ConversationKind.ENQUIRY, AISYAH, KEMAS_HOME, null,
                REQUESTED_AT, question)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Conversation.restore(id, null, AISYAH, KEMAS_HOME, null, REQUESTED_AT, question))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Conversation.restore(id, ConversationKind.ENQUIRY, null, KEMAS_HOME, null,
                REQUESTED_AT, question)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Conversation.restore(id, ConversationKind.ENQUIRY, AISYAH, null, null,
                REQUESTED_AT, question)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Conversation.restore(id, ConversationKind.ENQUIRY, AISYAH, KEMAS_HOME, null, null,
                question)).isInstanceOf(NullPointerException.class);
    }
}
