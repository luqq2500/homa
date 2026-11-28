package com.homa.conversation.usecase;

import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.CustomerStanding;
import com.homa.conversation.domain.Message;
import com.homa.conversation.domain.ProviderId;
import com.homa.conversation.usecase.dto.StartEnquiryRequest;
import com.homa.conversation.usecase.dto.StartEnquiryResponse;
import com.homa.conversation.usecase.exception.CustomerNotFoundException;
import com.homa.conversation.usecase.exception.ProviderNotFoundException;
import com.homa.conversation.usecase.port.in.EnquiryStarter;
import com.homa.conversation.usecase.port.out.ConversationRepository;
import com.homa.conversation.usecase.port.out.CustomerStandingRepository;
import com.homa.conversation.usecase.port.out.ProviderOwners;
import com.homa.conversation.usecase.port.out.TimeSource;

/**
 * CHT-1: a customer in good standing asks a provider a question before reserving, which starts an Enquiry. Only a
 * customer Conversation knows can start one, and only about a provider it knows; the domain refuses a suspended
 * customer (CUS-5). A new question is a new Enquiry (CHT-5), so nothing here looks for an earlier one. Publishes
 * nothing: no other context reacts to a chat.
 */
public final class CustomerStartEnquiry implements EnquiryStarter {

    private final CustomerStandingRepository customers;
    private final ProviderOwners providers;
    private final ConversationRepository conversations;
    private final TimeSource time;

    public CustomerStartEnquiry(CustomerStandingRepository customers, ProviderOwners providers,
            ConversationRepository conversations, TimeSource time) {
        this.customers = customers;
        this.providers = providers;
        this.conversations = conversations;
        this.time = time;
    }

    @Override
    public StartEnquiryResponse start(StartEnquiryRequest request) {
        CustomerStanding customer = customers.findById(new CustomerId(request.customerId()))
                .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));
        ProviderId providerId = new ProviderId(request.providerId());
        providers.ownerOf(providerId).orElseThrow(() -> new ProviderNotFoundException(request.providerId()));

        Conversation enquiry = Conversation.startEnquiry(customer, providerId, request.question(), time.now());
        conversations.save(enquiry);

        Message question = enquiry.messages().getFirst();
        return new StartEnquiryResponse(enquiry.id().value(), question.id().value(), question.sentAt());
    }
}
