package policy.usecase.port.in;

import policy.usecase.dto.ViewHomaValuesRequest;
import policy.usecase.dto.ViewHomaValuesResponse;

/** An admin views the Homa-set values and the changes made to them. */
public interface AdminHomaValuesViewer {

    ViewHomaValuesResponse view(ViewHomaValuesRequest request);
}
