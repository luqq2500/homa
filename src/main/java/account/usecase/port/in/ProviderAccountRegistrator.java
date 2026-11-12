package account.usecase.port.in;

import account.usecase.dto.RegisterProviderRequest;
import account.usecase.dto.RegisterProviderResponse;

/** A business registers with a confirmed phone number. */
public interface ProviderAccountRegistrator {

    RegisterProviderResponse register(RegisterProviderRequest request);
}
