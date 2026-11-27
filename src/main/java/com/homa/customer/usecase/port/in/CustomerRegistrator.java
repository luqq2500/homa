package com.homa.customer.usecase.port.in;

import com.homa.customer.usecase.dto.RegisterAccountRequest;
import com.homa.customer.usecase.dto.RegisterAccountResponse;

/**
 * Someone signed in through an identity provider registers as a customer, with a name, a home address with its city
 * and an arrival note (CUS-1), having agreed to the Terms of Service (CUS-2). One identity-provider account holds one
 * customer account (CUS-7).
 */
public interface CustomerRegistrator {

    RegisterAccountResponse register(RegisterAccountRequest request);
}
