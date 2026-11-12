package account.usecase.port.in;

import account.usecase.dto.RegisterCustomerRequest;
import account.usecase.dto.RegisterCustomerResponse;

/** A customer registers with a confirmed phone number, their name and their address. */
public interface CustomerAccountRegistrator {

    RegisterCustomerResponse register(RegisterCustomerRequest request);
}
