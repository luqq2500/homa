package catalogue.usecase.port.in;

import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.AddToolResponse;

/** An admin adds a tool to the tool list. */
public interface AdminToolAdder {

    AddToolResponse add(AddToolRequest request);
}
