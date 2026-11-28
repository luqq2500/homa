package com.homa.conversation.domain;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.DANIEL;
import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.domain.ConversationFixtures.MONDAY_9_00;
import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;
import static com.homa.conversation.domain.ConversationFixtures.aisyah;
import static com.homa.conversation.domain.ConversationFixtures.aisyahsEnquiry;
import static com.homa.conversation.domain.ConversationFixtures.requestedWindow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.exception.EmptyMessageException;
import com.homa.conversation.domain.exception.SuspendedCustomerCannotStartEnquiryException;
import com.homa.fixtures.HomaFixtures;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConversationEnquiryTest {

    private static final Instant ANSWERED_AT = REQUESTED_AT.plus(Duration.ofMinutes(10));

    @Test
    @Tag("CHT-1")
    @DisplayName("Aisyah asks Kemas Home about a ladder before booking.")
    void aCustomerStartsAnEnquiryByAskingTheirQuestion() {
        Conversation enquiry = Conversation.startEnquiry(aisyah(), KEMAS_HOME, "Do you bring a ladder?", REQUESTED_AT);

        assertThat(enquiry.kind()).isEqualTo(ConversationKind.ENQUIRY);
        assertThat(enquiry.customerId()).isEqualTo(AISYAH);
        assertThat(enquiry.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(enquiry.reservationId()).isEmpty();
        assertThat(enquiry.startedAt()).isEqualTo(REQUESTED_AT);
        assertThat(enquiry.id().value()).isNotBlank();
        assertThat(enquiry.messages()).hasSize(1);
        Message question = enquiry.messages().getFirst();
        assertThat(question.author()).isEqualTo(PartyRole.CUSTOMER);
        assertThat(question.text()).isEqualTo("Do you bring a ladder?");
        assertThat(question.sentAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Aisyah's husband registers and asks Kemas Home about a ladder, without verifying his identity: "
            + "enquiries don't need it.")
    void aCustomerWhoHasNotVerifiedStartsAnEnquiry() {
        CustomerStanding husband = CustomerStanding.registered(new CustomerId("customer-husband"));

        Conversation enquiry = Conversation.startEnquiry(husband, KEMAS_HOME, "Do you bring a ladder?", REQUESTED_AT);

        assertThat(enquiry.customerId()).isEqualTo(husband.customerId());
        assertThat(husband.identityVerified()).isFalse();
    }

    @Test
    @Tag("CHT-1")
    @Tag("CUS-5")
    @DisplayName("Support suspends Daniel for 30 days: a suspended customer can't start enquiries.")
    void aSuspendedCustomerCannotStartAnEnquiry() {
        CustomerStanding daniel = CustomerStanding.registered(DANIEL)
                .suspended(HomaFixtures.at(2026, 12, 1, 9, 0), HomaFixtures.at(2026, 12, 31, 9, 0));

        assertThatThrownBy(() -> Conversation.startEnquiry(daniel, KEMAS_HOME, "Are you free?",
                HomaFixtures.at(2026, 12, 10, 9, 0)))
                .isInstanceOf(SuspendedCustomerCannotStartEnquiryException.class);
    }

    @Test
    @Tag("CHT-1")
    @Tag("CUS-6")
    @DisplayName("Daniel can start enquiries again once his 30-day suspension has ended.")
    void aCustomerCanStartAnEnquiryOnceTheirSuspensionHasEnded() {
        Instant suspensionEnds = HomaFixtures.at(2026, 12, 31, 9, 0);
        CustomerStanding daniel = CustomerStanding.registered(DANIEL)
                .suspended(HomaFixtures.at(2026, 12, 1, 9, 0), suspensionEnds);

        Conversation enquiry = Conversation.startEnquiry(daniel, KEMAS_HOME, "Are you free?", suspensionEnds);

        assertThat(enquiry.customerId()).isEqualTo(DANIEL);
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("An Enquiry is started by asking something: a question with no words starts nothing.")
    void anEnquiryNeedsItsQuestion() {
        assertThatThrownBy(() -> Conversation.startEnquiry(aisyah(), KEMAS_HOME, " ", REQUESTED_AT))
                .isInstanceOf(EmptyMessageException.class);
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Kemas Home answers Aisyah's question about a ladder: the provider can only write to a customer who "
            + "has contacted it, so the answer follows the question.")
    void theProviderAnswersAnEnquiryTheCustomerStarted() {
        Conversation enquiry = aisyahsEnquiry();

        Message answer = enquiry.send(PartyRole.PROVIDER, "Yes, a 2-metre ladder.", Optional.empty(), ANSWERED_AT);

        assertThat(answer.author()).isEqualTo(PartyRole.PROVIDER);
        assertThat(answer.text()).isEqualTo("Yes, a 2-metre ladder.");
        assertThat(answer.sentAt()).isEqualTo(ANSWERED_AT);
        assertThat(enquiry.messages()).extracting(Message::author)
                .containsExactly(PartyRole.CUSTOMER, PartyRole.PROVIDER);
        assertThat(enquiry.messages().getLast()).isSameAs(answer);
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Aisyah and Kemas Home go on talking in the Enquiry, each message after the one before.")
    void bothPartiesKeepWritingInAnEnquiryInTheOrderTheySent() {
        Conversation enquiry = aisyahsEnquiry();

        enquiry.send(PartyRole.PROVIDER, "Yes.", Optional.empty(), ANSWERED_AT);
        enquiry.send(PartyRole.CUSTOMER, "Thank you!", Optional.empty(), ANSWERED_AT.plus(Duration.ofMinutes(1)));

        assertThat(enquiry.messages()).extracting(Message::text)
                .containsExactly("Do you bring a ladder?", "Yes.", "Thank you!");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("An Enquiry stays open for weeks: no Protection Window ends it, and a reservation's window is "
            + "not its business.")
    void anEnquiryIsNeverReadOnly() {
        Conversation enquiry = aisyahsEnquiry();

        ChatAccess access = enquiry.accessAt(Optional.empty(), MONDAY_9_00.plus(Duration.ofDays(60)));
        ChatAccess withAnEndedWindow = enquiry.accessAt(Optional.of(requestedWindow().withdrawn()), MONDAY_9_00);

        assertThat(access.isOpen()).isTrue();
        assertThat(access.readOnlyReason()).isEmpty();
        assertThat(access.openUntil()).isEmpty();
        assertThat(withAnEndedWindow.isOpen()).isTrue();
        assertThat(enquiry.send(PartyRole.CUSTOMER, "One more thing.", Optional.of(requestedWindow().withdrawn()),
                MONDAY_9_00).text()).isEqualTo("One more thing.");
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("New questions go in a new Enquiry: Aisyah's second one is a conversation of its own.")
    void aNewQuestionGoesInANewEnquiry() {
        Conversation first = aisyahsEnquiry();
        Conversation second = Conversation.startEnquiry(aisyah(), KEMAS_HOME, "Do you do windows?", MONDAY_9_00);

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(second.messages()).extracting(Message::text).containsExactly("Do you do windows?");
        assertThat(first.messages()).extracting(Message::text).containsExactly("Do you bring a ladder?");
    }

    @Test
    @DisplayName("A conversation's messages can't be changed from outside.")
    void aConversationsMessagesCannotBeChangedFromOutside() {
        Conversation enquiry = aisyahsEnquiry();

        assertThatThrownBy(() -> enquiry.messages().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("A conversation needs a customer in good standing, a provider and a time.")
    void anEnquiryNeedsItsPartiesAndTime() {
        assertThatThrownBy(() -> Conversation.startEnquiry(aisyah(), null, "Hi", REQUESTED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Conversation.startEnquiry(aisyah(), KEMAS_HOME, "Hi", null))
                .isInstanceOf(NullPointerException.class);
    }
}
