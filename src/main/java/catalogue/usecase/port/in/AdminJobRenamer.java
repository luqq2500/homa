package catalogue.usecase.port.in;

import catalogue.usecase.dto.RenameJobRequest;
import catalogue.usecase.dto.RenameJobResponse;

/** An admin renames a job; providers keep it and only what is shown changes. */
public interface AdminJobRenamer {

    RenameJobResponse rename(RenameJobRequest request);
}
