package catalogue.usecase.port.in;

import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddJobResponse;

/** An admin adds a job to the job list. */
public interface AdminJobAdder {

    AddJobResponse add(AddJobRequest request);
}
