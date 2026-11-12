package conversation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import conversation.domain.exception.ConversationAlreadyLinkedException;
import conversation.domain.exception.ConversationDetailRequiredException;
import conversation.domain.exception.ReservationChatCannotBeArchivedException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Feature: Requesting opens the reservation's chat. */
class ReservationChatOpeningTest {

    private static final Instant MONDAY = Instant.parse("2026-11-09T02:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-09T03:00:00Z");

    @Test
    void aDirectRequestOpensTheChatWithNoMessage() {
        Conversation chat = Conversation.openForReservation("reservation-1", "customer-1", "provider-1", MONDAY);

        assertThat(chat.subject()).isEqualTo(ChatSubject.reservation("reservation-1"));
        assertThat(chat.messages()).isEmpty();
        assertThat(chat.customerId()).isEqualTo("customer-1");
        assertThat(chat.providerId()).isEqualTo("provider-1");
        assertThat(chat.isArchived()).isFalse();
        assertThat(chat.id()).isNotBlank();
    }

    @Test
    void theProviderCanWriteFirstInAReservationsChat() {
        Conversation chat = Conversation.openForReservation("reservation-1", "customer-1", "provider-1", MONDAY);

        chat.post(Party.PROVIDER, "Running 10 minutes late", LATER);

        assertThat(chat.messages()).hasSize(1);
        assertThat(chat.messages().get(0).sender()).isEqualTo(Party.PROVIDER);
    }

    @Test
    void aReservationsChatIsNeverArchivedOrLinkedAgain() {
        Conversation chat = Conversation.openForReservation("reservation-1", "customer-1", "provider-1", MONDAY);

        assertThatThrownBy(() -> chat.archive(LATER)).isInstanceOf(ReservationChatCannotBeArchivedException.class);
        assertThatThrownBy(() -> chat.linkReservation("reservation-2")).isInstanceOf(ConversationAlreadyLinkedException.class);
    }

    @Test
    void theChatNeedsAReservationACustomerAProviderAndATime() {
        assertThatThrownBy(() -> Conversation.openForReservation(" ", "customer-1", "provider-1", MONDAY))
                .isInstanceOf(ConversationDetailRequiredException.class);
        assertThatThrownBy(() -> Conversation.openForReservation("reservation-1", " ", "provider-1", MONDAY))
                .isInstanceOf(ConversationDetailRequiredException.class)
                .hasMessageContaining("customer");
        assertThatThrownBy(() -> Conversation.openForReservation("reservation-1", "customer-1", " ", MONDAY))
                .isInstanceOf(ConversationDetailRequiredException.class)
                .hasMessageContaining("provider");
        assertThatThrownBy(() -> Conversation.openForReservation("reservation-1", "customer-1", "provider-1", null))
                .isInstanceOf(NullPointerException.class);
    }
}
