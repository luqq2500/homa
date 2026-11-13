package catalogue.usecase.port.in;

import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.RetireToolResponse;

/** An admin retires a tool, with a reason, so providers can no longer pick it. */
public interface AdminToolRetirer {

    RetireToolResponse retire(RetireToolRequest request);
}
