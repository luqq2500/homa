package account.usecase.port.in;

import account.usecase.dto.RegisterCustomerRequest;
import account.usecase.dto.RegisterCustomerResponse;

/** A guest with a verified phone number registers as a customer. */
public interface GuestCustomerRegistrator {

    RegisterCustomerResponse register(RegisterCustomerRequest request);
}
