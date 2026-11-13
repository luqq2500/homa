package availability.usecase.port.in;

import availability.usecase.dto.SetBufferRequest;
import availability.usecase.dto.SetBufferResponse;

/** A provider sets the buffer kept free before and after each accepted Homa job. */
public interface ProviderBufferSetter {

    SetBufferResponse set(SetBufferRequest request);
}
