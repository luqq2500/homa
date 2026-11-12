package catalogue.usecase.port.in;

import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.AddToolResponse;

/** Homa's founders add a tool to the tool list. */
public interface HomaToolAdder {

    AddToolResponse add(AddToolRequest request);
}
