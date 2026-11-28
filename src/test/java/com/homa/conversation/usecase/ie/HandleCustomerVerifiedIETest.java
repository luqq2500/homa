package com.homa.conversation.usecase.ie;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.DANIEL;
import static com.homa.conversation.usecase.ConversationTestBed.AISYAH_ID;
import static com.homa.conversation.usecase.ConversationTestBed.KEMAS_HOME_ID;
import static com.homa.conversation.usecase.ConversationTestBed.customerVerifiedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.CustomerStanding;
import com.homa.conversation.domain.exception.SuspendedCustomerCannotStartEnquiryException;
import com.homa.conversation.usecase.ConversationTestBed;
import com.homa.conversation.usecase.dto.StartEnquiryRequest;
import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.CustomerVerifiedIE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleCustomerVerifiedIETest {

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("CHT-1")
    @Tag("CUS-3")
    @DisplayName("Aisyah verifies her identity at her first reservation: Conversation keeps the fact, and she can "
            + "still ask Kemas Home questions, as she could before.")
    void aVerifiedCustomerIsKeptVerifiedAndStaysInGoodStanding() {
        bed.customerVerified.handle(customerVerifiedEvent("verified-1", AISYAH_ID));

        assertThat(bed.customers.findById(AISYAH).orElseThrow().identityVerified()).isTrue();
        assertThat(bed.startEnquiry.start(new StartEnquiryRequest(AISYAH_ID, KEMAS_HOME_ID, "A ladder?"))
                .conversationId()).isNotBlank();
    }

    @Test
    @Tag("CHT-1")
    @Tag("CUS-5")
    @DisplayName("Daniel, suspended until Support lifts it, verifies his identity: he is verified, and still can't "
            + "start enquiries.")
    void verifyingASuspendedCustomerDoesNotLiftTheSuspension() {
        bed.customers.given(CustomerStanding.registered(DANIEL).suspended(HomaFixtures.at(2026, 12, 1, 9, 0), null));
        bed.time.setNow(HomaFixtures.at(2026, 12, 2, 9, 0));

        bed.customerVerified.handle(customerVerifiedEvent("verified-1", DANIEL.value()));

        CustomerStanding daniel = bed.customers.findById(DANIEL).orElseThrow();
        assertThat(daniel.identityVerified()).isTrue();
        assertThat(daniel.suspension()).isPresent();
        assertThatThrownBy(() -> bed.startEnquiry.start(new StartEnquiryRequest(DANIEL.value(), KEMAS_HOME_ID, "Hi")))
                .isInstanceOf(SuspendedCustomerCannotStartEnquiryException.class);
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("The relay delivers the verification again: nothing changes.")
    void aRedeliveredVerificationChangesNothing() {
        CustomerVerifiedIE event = customerVerifiedEvent("verified-1", AISYAH_ID);
        bed.customerVerified.handle(event);
        int saves = bed.customers.saves();
        bed.customers.given(CustomerStanding.registered(AISYAH));

        bed.customerVerified.handle(event);

        assertThat(bed.customers.saves()).isEqualTo(saves);
        assertThat(bed.customers.findById(AISYAH).orElseThrow().identityVerified()).isFalse();
        assertThat(bed.processedMessages.hasHandled("verified-1", HandleCustomerVerifiedIE.HANDLER)).isTrue();
        assertThat(HandleCustomerVerifiedIE.HANDLER).isEqualTo("conversation.CustomerVerified");
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("Out of order: a verification is heard for a customer whose registration hasn't arrived. The standing "
            + "is created verified, not lost, and the customer is in good standing.")
    void aVerificationHeardBeforeTheRegistrationIsKept() {
        int known = bed.customers.count();

        bed.customerVerified.handle(customerVerifiedEvent("verified-1", "customer-husband"));

        assertThat(bed.customers.count()).isEqualTo(known + 1);
        CustomerStanding husband = bed.customers.findById(new CustomerId("customer-husband")).orElseThrow();
        assertThat(husband.identityVerified()).isTrue();
        assertThat(husband.suspension()).isEmpty();
    }
}
