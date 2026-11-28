package com.homa.conversation.domain;

import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.exception.EmptyMessageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MessageTest {

    @Test
    @Tag("CHT-3")
    @DisplayName("Kemas Home offers a cash discount: the message is kept exactly as written, because Homa's "
            + "reminder is the apps' copy, not something that edits or filters what a party says.")
    void aMessageIsKeptExactlyAsWritten() {
        String offer = "  Pay cash on the day and I'll take RM20 off!  ";

        Message message = Message.sentBy(PartyRole.PROVIDER, offer, REQUESTED_AT);

        assertThat(message.text()).isEqualTo(offer);
        assertThat(message.author()).isEqualTo(PartyRole.PROVIDER);
        assertThat(message.sentAt()).isEqualTo(REQUESTED_AT);
        assertThat(message.id().value()).isNotBlank();
    }

    @Test
    @DisplayName("Every new message has an id of its own.")
    void everyNewMessageHasItsOwnId() {
        Message first = Message.sentBy(PartyRole.CUSTOMER, "Hello", REQUESTED_AT);
        Message second = Message.sentBy(PartyRole.CUSTOMER, "Hello", REQUESTED_AT);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @ParameterizedTest(name = "[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t "})
    @DisplayName("A message with no words is not a message.")
    void aMessageNeedsSomeText(String text) {
        assertThatThrownBy(() -> Message.sentBy(PartyRole.CUSTOMER, text, REQUESTED_AT))
                .isInstanceOf(EmptyMessageException.class);
    }

    @Test
    @DisplayName("A message names who wrote it, when, and its id.")
    void aMessageNamesItsAuthorTimeAndId() {
        assertThatThrownBy(() -> new Message(null, PartyRole.CUSTOMER, "Hi", REQUESTED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Message(MessageId.generate(), null, "Hi", REQUESTED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Message(MessageId.generate(), PartyRole.CUSTOMER, "Hi", null))
                .isInstanceOf(NullPointerException.class);
    }
}
