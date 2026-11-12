package catalogue.usecase.port.in;

import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.RetireToolResponse;

/** Homa's founders retire a tool, so providers can no longer say they bring it. */
public interface HomaToolRetirer {

    RetireToolResponse retire(RetireToolRequest request);
}
