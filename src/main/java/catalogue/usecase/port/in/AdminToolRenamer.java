package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;

/** An admin renames a tool. */
public interface AdminToolRenamer {

    RenameToolResponse rename(RenameToolRequest request);
}
