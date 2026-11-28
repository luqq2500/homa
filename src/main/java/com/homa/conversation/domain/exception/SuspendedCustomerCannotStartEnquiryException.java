package com.homa.conversation.domain.exception;

/** CHT-1, CUS-5: a suspended customer can't start enquiries. */
public final class SuspendedCustomerCannotStartEnquiryException extends ConversationDomainException {

    private static final long serialVersionUID = 1L;

    public SuspendedCustomerCannotStartEnquiryException(String customerId) {
        super("CUS-5: customer " + customerId + " is suspended and cannot start an enquiry");
    }
}
