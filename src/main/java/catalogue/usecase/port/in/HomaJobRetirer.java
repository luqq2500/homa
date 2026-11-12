package catalogue.usecase.port.in;

import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireJobResponse;

/** Homa's founders retire a job, so providers can no longer offer it. */
public interface HomaJobRetirer {

    RetireJobResponse retire(RetireJobRequest request);
}
