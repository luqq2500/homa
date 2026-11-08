package catalogue.usecase.port.in;

import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.RetireToolResponse;

/** Homa retires a tool from its tool list. */
public interface HomaToolRetirer {

    RetireToolResponse retire(RetireToolRequest request);
}
