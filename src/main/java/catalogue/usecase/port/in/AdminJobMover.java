package catalogue.usecase.port.in;

import catalogue.usecase.dto.MoveJobRequest;
import catalogue.usecase.dto.MoveJobResponse;

/** An admin moves a job to another group. */
public interface AdminJobMover {

    MoveJobResponse move(MoveJobRequest request);
}
