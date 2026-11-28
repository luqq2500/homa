package com.homa.conversation.usecase.ie;

import static com.homa.conversation.domain.ConversationFixtures.DANIEL;
import static com.homa.conversation.usecase.ConversationTestBed.KEMAS_HOME_ID;
import static com.homa.conversation.usecase.ConversationTestBed.customerRegisteredEvent;
import static com.homa.conversation.usecase.ConversationTestBed.customerVerifiedEvent;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.CustomerStanding;
import com.homa.conversation.usecase.ConversationTestBed;
import com.homa.conversation.usecase.dto.StartEnquiryRequest;
import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.CustomerRegisteredIE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleCustomerRegisteredIETest {

    private static final String HUSBAND_ID = "customer-husband";
    private static final CustomerId HUSBAND = new CustomerId(HUSBAND_ID);

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("CHT-1")
    @DisplayName("Aisyah's husband registers: he is in good standing, though not identity verified, so he can ask Kemas "
            + "Home about a ladder at once.")
    void aRegisteredCustomerIsInGoodStandingAndCanStartEnquiries() {
        int known = bed.customers.count();

        bed.customerRegistered.handle(customerRegisteredEvent("registered-1", HUSBAND_ID));

        CustomerStanding husband = bed.customers.findById(HUSBAND).orElseThrow();
        assertThat(bed.customers.count()).isEqualTo(known + 1);
        assertThat(husband.identityVerified()).isFalse();
        assertThat(husband.suspension()).isEmpty();
        assertThat(bed.startEnquiry.start(new StartEnquiryRequest(HUSBAND_ID, KEMAS_HOME_ID, "A ladder?"))
                .conversationId()).isNotBlank();
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("The relay delivers the registration again: nothing changes.")
    void aRedeliveredRegistrationChangesNothing() {
        CustomerRegisteredIE event = customerRegisteredEvent("registered-1", HUSBAND_ID);
        bed.customerRegistered.handle(event);
        int saves = bed.customers.saves();
        bed.customers.given(bed.customers.findById(HUSBAND).orElseThrow().verified());

        bed.customerRegistered.handle(event);

        assertThat(bed.customers.saves()).isEqualTo(saves);
        assertThat(bed.customers.findById(HUSBAND).orElseThrow().identityVerified()).isTrue();
        assertThat(bed.processedMessages.hasHandled("registered-1", HandleCustomerRegisteredIE.HANDLER)).isTrue();
        assertThat(HandleCustomerRegisteredIE.HANDLER).isEqualTo("conversation.CustomerRegistered");
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("Out of order: the husband's verification is heard before his registration. The registration leaves "
            + "the standing as it is, so the verification is not lost.")
    void aRegistrationHeardAfterTheVerificationKeepsTheVerification() {
        bed.customerVerified.handle(customerVerifiedEvent("verified-1", HUSBAND_ID));

        bed.customerRegistered.handle(customerRegisteredEvent("registered-1", HUSBAND_ID));

        assertThat(bed.customers.findById(HUSBAND).orElseThrow().identityVerified()).isTrue();
    }

    @Test
    @Tag("CUS-5")
    @DisplayName("Registering does not lift a suspension: a registration heard again, as another event, leaves a "
            + "suspended customer suspended.")
    void aRegistrationDoesNotChangeAnExistingStanding() {
        bed.customers.given(CustomerStanding.registered(DANIEL)
                .suspended(HomaFixtures.at(2026, 12, 1, 9, 0), null));

        bed.customerRegistered.handle(customerRegisteredEvent("registered-2", DANIEL.value()));

        assertThat(bed.customers.findById(DANIEL).orElseThrow().suspension()).isPresent();
    }
}
