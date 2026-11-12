package catalogue.usecase.port.in;

import catalogue.usecase.dto.RegroupJobRequest;
import catalogue.usecase.dto.RegroupJobResponse;

/** Homa's founders move a job to another display group. */
public interface HomaJobRegrouper {

    RegroupJobResponse regroup(RegroupJobRequest request);
}
