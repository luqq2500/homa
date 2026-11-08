package catalogue.usecase.port.in;

import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.AddToolResponse;

/** Homa adds a tool to its tool list. */
public interface HomaToolAdder {

    AddToolResponse add(AddToolRequest request);
}
