package account.usecase.port.in;

import account.usecase.dto.RegisterBusinessRequest;
import account.usecase.dto.RegisterBusinessResponse;

/** Someone with a verified phone number registers a business. */
public interface GuestBusinessRegistrator {

    RegisterBusinessResponse register(RegisterBusinessRequest request);
}
