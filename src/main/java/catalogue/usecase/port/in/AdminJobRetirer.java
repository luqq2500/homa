package catalogue.usecase.port.in;

import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;

/** An admin retires a job, with a reason, so providers can no longer pick it. */
public interface AdminJobRetirer {

    RetireJobResponse retire(RetireJobRequest request);
}
