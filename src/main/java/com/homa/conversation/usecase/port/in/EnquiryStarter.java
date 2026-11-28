package com.homa.conversation.usecase.port.in;

import com.homa.conversation.usecase.dto.StartEnquiryRequest;
import com.homa.conversation.usecase.dto.StartEnquiryResponse;

/** CHT-1: a customer starts an Enquiry with a provider by asking their question. */
public interface EnquiryStarter {

    StartEnquiryResponse start(StartEnquiryRequest request);
}
