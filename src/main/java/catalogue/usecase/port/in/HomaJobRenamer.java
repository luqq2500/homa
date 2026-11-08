package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;

/** Homa renames a job on its job list. */
public interface HomaJobRenamer {

    RenameJobResponse rename(RenameJobRequest request);
}
