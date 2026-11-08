package catalogue.usecase.port.in;

import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddJobResponse;

/** Homa adds a job to its job list. */
public interface HomaJobAdder {

    AddJobResponse add(AddJobRequest request);
}
