package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;

/** Homa renames a tool on its tool list. */
public interface HomaToolRenamer {

    RenameToolResponse rename(RenameToolRequest request);
}
