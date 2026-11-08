package conversation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import conversation.domain.exception.ConversationAlreadyLinkedException;
import conversation.domain.exception.ConversationArchivedException;
import conversation.domain.exception.ConversationDetailRequiredException;
import conversation.domain.exception.MessageTextRequiredException;
import conversation.domain.exception.OnlyCustomerCanStartChatException;
import conversation.domain.exception.ReservationChatCannotBeArchivedException;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Feature: Chat. */
class ConversationChatTest {

    private static final Instant MONDAY = Instant.parse("2026-11-09T02:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-09T03:00:00Z");

    private static Conversation aChatForEnquiry(String enquiryId) {
        return Conversation.startByCustomer(ChatSubject.enquiry(enquiryId), "customer-1", "provider-1", "Do you bring a steam mop?", MONDAY);
    }

    @Nested
    class Starting {

        @Test
        void onlyTheCustomerCanStartAChat() {
            assertThatThrownBy(() -> Conversation.start(
                            Party.PROVIDER, ChatSubject.enquiry("E1"), "customer-1", "provider-1", "Hello", MONDAY))
                    .isInstanceOf(OnlyCustomerCanStartChatException.class);
        }

        @Test
        void aChatStartsWithTheCustomersFirstMessage() {
            Conversation chat = aChatForEnquiry("E1");

            assertThat(chat.messages()).hasSize(1);
            Message first = chat.messages().get(0);
            assertThat(first.sender()).isEqualTo(Party.CUSTOMER);
            assertThat(first.text()).isEqualTo("Do you bring a steam mop?");
            assertThat(first.sentAt()).isEqualTo(MONDAY);
            assertThat(first.id()).isNotBlank();
            assertThat(chat.subject()).isEqualTo(ChatSubject.enquiry("E1"));
            assertThat(chat.customerId()).isEqualTo("customer-1");
            assertThat(chat.providerId()).isEqualTo("provider-1");
            assertThat(chat.id()).isNotBlank();
            assertThat(chat.isArchived()).isFalse();
            assertThat(chat.archivedAt()).isEmpty();
        }

        @Test
        void aChatIsBetweenACustomerAndAProviderAboutSomething() {
            assertThatThrownBy(() -> Conversation.startByCustomer(ChatSubject.enquiry("E1"), " ", "provider-1", "Hi", MONDAY))
                    .isInstanceOf(ConversationDetailRequiredException.class);
            assertThatThrownBy(() -> Conversation.startByCustomer(ChatSubject.enquiry("E1"), "customer-1", null, "Hi", MONDAY))
                    .isInstanceOf(ConversationDetailRequiredException.class);
            assertThatThrownBy(() -> ChatSubject.enquiry(" ")).isInstanceOf(ConversationDetailRequiredException.class);
            assertThatThrownBy(() -> new ChatSubject(null, "E1")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Conversation.startByCustomer(null, "customer-1", "provider-1", "Hi", MONDAY))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Posting {

        @Test
        void theProviderCanReplyOnceTheCustomerHasStarted() {
            Conversation chat = aChatForEnquiry("E1");

            chat.post(Party.PROVIDER, " Yes, I can bring a steam mop ", LATER);

            assertThat(chat.messages()).hasSize(2);
            assertThat(chat.messages().get(1).sender()).isEqualTo(Party.PROVIDER);
            assertThat(chat.messages().get(1).text()).isEqualTo("Yes, I can bring a steam mop");
        }

        @Test
        void aMessageNeedsTextASenderAndATime() {
            Conversation chat = aChatForEnquiry("E1");

            assertThatThrownBy(() -> chat.post(Party.PROVIDER, " ", LATER)).isInstanceOf(MessageTextRequiredException.class);
            assertThatThrownBy(() -> chat.post(Party.PROVIDER, null, LATER)).isInstanceOf(MessageTextRequiredException.class);
            assertThatThrownBy(() -> chat.post(null, "Hi", LATER)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> chat.post(Party.PROVIDER, "Hi", null)).isInstanceOf(NullPointerException.class);
            assertThat(chat.messages()).hasSize(1);
        }

        @Test
        void theMessagesShownCannotBeChangedFromOutside() {
            assertThatThrownBy(() -> aChatForEnquiry("E1").messages().clear()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class EnquiriesAndReservations {

        @Test
        void requestingFromAnEnquiryKeepsTheSameChat() {
            Conversation chat = aChatForEnquiry("E1");
            chat.post(Party.PROVIDER, "Yes, I can bring a steam mop", LATER);

            chat.linkReservation("R1");

            assertThat(chat.subject()).isEqualTo(ChatSubject.reservation("R1"));
            assertThat(chat.messages()).hasSize(2);
        }

        @Test
        void aReservationsChatIsNotLinkedAgain() {
            Conversation chat = aChatForEnquiry("E1");
            chat.linkReservation("R1");

            assertThatThrownBy(() -> chat.linkReservation("R2"))
                    .isInstanceOf(ConversationAlreadyLinkedException.class)
                    .hasMessageContaining("R1");
        }

        @Test
        void anArchivedChatIsReadableButClosed() {
            Conversation chat = aChatForEnquiry("E1");

            chat.archive(LATER);

            assertThat(chat.isArchived()).isTrue();
            assertThat(chat.archivedAt()).contains(LATER);
            assertThat(chat.messages()).hasSize(1);
            assertThatThrownBy(() -> chat.post(Party.CUSTOMER, "Are you free next week?", LATER))
                    .isInstanceOf(ConversationArchivedException.class);
        }

        @Test
        void aChatIsArchivedOnce() {
            Conversation chat = aChatForEnquiry("E1");
            chat.archive(LATER);

            assertThatThrownBy(() -> chat.archive(LATER)).isInstanceOf(ConversationArchivedException.class);
        }

        @Test
        void aReservationsChatStaysOpenAfterTheJob() {
            Conversation chat = Conversation.startByCustomer(
                    ChatSubject.reservation("R1"), "customer-1", "provider-1", "See you Saturday", MONDAY);

            chat.post(Party.CUSTOMER, "I think I left my keys in the kitchen", LATER);

            assertThat(chat.messages()).hasSize(2);
            assertThatThrownBy(() -> chat.archive(LATER)).isInstanceOf(ReservationChatCannotBeArchivedException.class);
        }
    }
}
