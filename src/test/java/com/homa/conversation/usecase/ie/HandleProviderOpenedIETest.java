package com.homa.conversation.usecase.ie;

import static com.homa.conversation.domain.ConversationFixtures.KEMAS_HOME;
import static com.homa.conversation.usecase.ConversationTestBed.AISYAH_ID;
import static com.homa.conversation.usecase.ConversationTestBed.AZMAN_PERSON_ID;
import static com.homa.conversation.usecase.ConversationTestBed.KEMAS_HOME_ID;
import static com.homa.conversation.usecase.ConversationTestBed.providerOpenedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.ProviderId;
import com.homa.conversation.usecase.ConversationTestBed;
import com.homa.conversation.usecase.dto.SendMessageRequest;
import com.homa.conversation.usecase.dto.StartEnquiryRequest;
import com.homa.conversation.usecase.exception.NotConversationPartyException;
import com.homa.conversation.usecase.exception.ProviderNotFoundException;
import com.homa.ie.events.ProviderOpenedIE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProviderOpenedIETest {

    private static final String HANA_PROVIDER_ID = "provider-hana-cleaning";
    private static final String HANA_PERSON_ID = "person-hana";

    private final ConversationTestBed bed = ConversationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("CHT-1")
    @DisplayName("Hana opens Hana Cleaning: Aisyah can now ask it a question, and Hana is the one who answers for it.")
    void anOpenedProviderCanBeAskedAndItsOwnerAnswers() {
        assertThatThrownBy(() -> bed.startEnquiry.start(new StartEnquiryRequest(AISYAH_ID, HANA_PROVIDER_ID, "Hi")))
                .isInstanceOf(ProviderNotFoundException.class);

        bed.providerOpened.handle(providerOpenedEvent("opened-1", HANA_PROVIDER_ID, HANA_PERSON_ID));

        assertThat(bed.owners.ownerOf(new ProviderId(HANA_PROVIDER_ID))).contains(HANA_PERSON_ID);
        String enquiryId = bed.startEnquiry.start(new StartEnquiryRequest(AISYAH_ID, HANA_PROVIDER_ID,
                "Do you clean ovens?")).conversationId();
        assertThat(bed.sendMessage.send(new SendMessageRequest(enquiryId, HANA_PERSON_ID, "Yes, we do.")).author())
                .isEqualTo("PROVIDER");
        assertThatThrownBy(() -> bed.sendMessage.send(new SendMessageRequest(enquiryId, AZMAN_PERSON_ID, "Hello")))
                .isInstanceOf(NotConversationPartyException.class);
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("The relay delivers the opening again: nothing changes.")
    void aRedeliveredOpeningChangesNothing() {
        ProviderOpenedIE event = providerOpenedEvent("opened-1", HANA_PROVIDER_ID, HANA_PERSON_ID);
        bed.providerOpened.handle(event);
        bed.owners.record(new ProviderId(HANA_PROVIDER_ID), "person-someone-else");

        bed.providerOpened.handle(event);

        assertThat(bed.owners.ownerOf(new ProviderId(HANA_PROVIDER_ID))).contains("person-someone-else");
        assertThat(bed.processedMessages.hasHandled("opened-1", HandleProviderOpenedIE.HANDLER)).isTrue();
        assertThat(HandleProviderOpenedIE.HANDLER).isEqualTo("conversation.ProviderOpened");
    }

    @Test
    @DisplayName("Kemas Home's owner stays Azman when another provider opens.")
    void anotherProvidersOpeningLeavesTheOwnersOfTheOthersAsTheyWere() {
        int known = bed.owners.count();

        bed.providerOpened.handle(providerOpenedEvent("opened-1", HANA_PROVIDER_ID, HANA_PERSON_ID));

        assertThat(bed.owners.count()).isEqualTo(known + 1);
        assertThat(bed.owners.ownerOf(KEMAS_HOME)).contains(AZMAN_PERSON_ID);
        assertThat(KEMAS_HOME.value()).isEqualTo(KEMAS_HOME_ID);
    }
}
