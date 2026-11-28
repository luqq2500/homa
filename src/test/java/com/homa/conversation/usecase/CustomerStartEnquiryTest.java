package com.homa.conversation.usecase;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.DANIEL;
import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;
import static com.homa.conversation.usecase.ConversationTestBed.AISYAH_ID;
import static com.homa.conversation.usecase.ConversationTestBed.AZMAN_PERSON_ID;
import static com.homa.conversation.usecase.ConversationTestBed.KEMAS_HOME_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.ConversationId;
import com.homa.conversation.domain.ConversationKind;
import com.homa.conversation.domain.CustomerStanding;
import com.homa.conversation.domain.Message;
import com.homa.conversation.domain.PartyRole;
import com.homa.conversation.domain.exception.EmptyMessageException;
import com.homa.conversation.domain.exception.SuspendedCustomerCannotStartEnquiryException;
import com.homa.conversation.usecase.dto.StartEnquiryRequest;
import com.homa.conversation.usecase.dto.StartEnquiryResponse;
import com.homa.conversation.usecase.exception.CustomerNotFoundException;
import com.homa.conversation.usecase.exception.ProviderNotFoundException;
import com.homa.fixtures.HomaFixtures;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerStartEnquiryTest {

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();

    private StartEnquiryRequest aisyahAsks(String question) {
        return new StartEnquiryRequest(AISYAH_ID, KEMAS_HOME_ID, question);
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Aisyah asks Kemas Home about a ladder before booking.")
    void aCustomerStartsAnEnquiryByAskingAProviderAQuestion() {
        StartEnquiryResponse response = bed.startEnquiry.start(aisyahAsks("Do you bring a ladder?"));

        Conversation enquiry = bed.conversations.findById(new ConversationId(response.conversationId()))
                .orElseThrow();
        assertThat(bed.conversations.saves()).containsExactly(enquiry.id());
        assertThat(enquiry.kind()).isEqualTo(ConversationKind.ENQUIRY);
        assertThat(enquiry.customerId()).isEqualTo(AISYAH);
        assertThat(enquiry.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(enquiry.startedAt()).isEqualTo(REQUESTED_AT);
        assertThat(enquiry.messages()).hasSize(1);
        Message question = enquiry.messages().getFirst();
        assertThat(question.author()).isEqualTo(PartyRole.CUSTOMER);
        assertThat(question.text()).isEqualTo("Do you bring a ladder?");
        assertThat(response.messageId()).isEqualTo(question.id().value());
        assertThat(response.startedAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    @Tag("CHT-1")
    @Tag("CUS-3")
    @DisplayName("Aisyah's husband registers and asks Kemas Home about a ladder, before he has verified his identity: "
            + "browsing and enquiries don't need it.")
    void aCustomerWhoHasNotVerifiedCanStartAnEnquiry() {
        bed.customerRegistered.handle(ConversationTestBed.customerRegisteredEvent("registered-husband",
                "customer-husband"));

        StartEnquiryResponse response = bed.startEnquiry.start(
                new StartEnquiryRequest("customer-husband", KEMAS_HOME_ID, "Do you bring a ladder?"));

        assertThat(bed.conversations.findById(new ConversationId(response.conversationId()))).isPresent();
    }

    @Test
    @Tag("CHT-1")
    @Tag("CUS-5")
    @DisplayName("Support suspends Daniel for 30 days: a suspended customer can't start enquiries, and nothing is "
            + "saved.")
    void aSuspendedCustomerCannotStartAnEnquiry() {
        bed.customers.given(CustomerStanding.registered(DANIEL)
                .suspended(HomaFixtures.at(2026, 12, 1, 9, 0), HomaFixtures.at(2026, 12, 31, 9, 0)));
        bed.time.setNow(HomaFixtures.at(2026, 12, 10, 9, 0));

        assertThatThrownBy(() -> bed.startEnquiry.start(
                new StartEnquiryRequest(DANIEL.value(), KEMAS_HOME_ID, "Are you free on Saturday?")))
                .isInstanceOf(SuspendedCustomerCannotStartEnquiryException.class);
        assertThat(bed.conversations.all()).isEmpty();
    }

    @Test
    @Tag("CHT-1")
    @Tag("CUS-6")
    @DisplayName("Daniel can ask again after 30 days: the suspension ends by itself.")
    void aCustomerCanStartAnEnquiryOnceTheirSuspensionHasEnded() {
        Instant suspensionEnds = HomaFixtures.at(2026, 12, 31, 9, 0);
        bed.customers.given(CustomerStanding.registered(DANIEL)
                .suspended(HomaFixtures.at(2026, 12, 1, 9, 0), suspensionEnds));
        bed.time.setNow(suspensionEnds);

        StartEnquiryResponse response = bed.startEnquiry.start(
                new StartEnquiryRequest(DANIEL.value(), KEMAS_HOME_ID, "Are you free on Saturday?"));

        assertThat(response.startedAt()).isEqualTo(suspensionEnds);
        assertThat(bed.conversations.all()).hasSize(1);
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Kemas Home can't start a chat with a customer: only a customer Conversation knows can ask, so "
            + "Azman asking as Aisyah's provider starts nothing.")
    void onlyACustomerCanStartAChat() {
        assertThatThrownBy(() -> bed.startEnquiry.start(new StartEnquiryRequest(AZMAN_PERSON_ID, KEMAS_HOME_ID,
                "Do you want a discount?")))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(AZMAN_PERSON_ID);
        assertThat(bed.conversations.all()).isEmpty();
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Aisyah asks a provider that Conversation has never heard of: there is no one to ask, and nothing "
            + "is saved.")
    void anEnquiryNeedsAProviderConversationKnows() {
        assertThatThrownBy(() -> bed.startEnquiry.start(new StartEnquiryRequest(AISYAH_ID, "provider-unknown", "Hi")))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining("provider-unknown");
        assertThat(bed.conversations.all()).isEmpty();
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("An Enquiry begins with a question: Aisyah can't start one with an empty message.")
    void anEnquiryNeedsItsQuestion() {
        assertThatThrownBy(() -> bed.startEnquiry.start(aisyahAsks("  ")))
                .isInstanceOf(EmptyMessageException.class);
        assertThat(bed.conversations.all()).isEmpty();
    }

    @Test
    @Tag("CHT-5")
    @DisplayName("New questions go in a new Enquiry: Aisyah asks Kemas Home about windows after the ladder, and gets "
            + "a second conversation.")
    void aNewQuestionStartsANewEnquiry() {
        StartEnquiryResponse ladder = bed.startEnquiry.start(aisyahAsks("Do you bring a ladder?"));
        StartEnquiryResponse windows = bed.startEnquiry.start(aisyahAsks("Do you clean windows?"));

        assertThat(windows.conversationId()).isNotEqualTo(ladder.conversationId());
        assertThat(bed.conversations.all()).hasSize(2);
    }
}
