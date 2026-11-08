package catalogue.usecase.port.in;

import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;

/** Homa retires a job from its job list. */
public interface HomaJobRetirer {

    RetireJobResponse retire(RetireJobRequest request);
}
