package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;

/** Homa's founders rename a tool on the tool list. */
public interface HomaToolRenamer {

    RenameToolResponse rename(RenameToolRequest request);
}
