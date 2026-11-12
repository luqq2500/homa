package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;

/** Homa's founders rename a job on the job list. */
public interface HomaJobRenamer {

    RenameJobResponse rename(RenameJobRequest request);
}
