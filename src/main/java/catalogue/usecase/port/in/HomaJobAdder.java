package catalogue.usecase.port.in;

import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddJobResponse;

/** Homa's founders add a job to the job list. */
public interface HomaJobAdder {

    AddJobResponse add(AddJobRequest request);
}
