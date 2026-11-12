package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameShelfRequest;
import catalogue.usecase.dto.RenameShelfResponse;

/** Homa's founders rename a homepage shelf. */
public interface HomaShelfRenamer {

    RenameShelfResponse rename(RenameShelfRequest request);
}
