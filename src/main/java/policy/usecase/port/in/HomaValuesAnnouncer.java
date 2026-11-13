package policy.usecase.port.in;

import policy.usecase.dto.AnnounceHomaValuesResponse;

/** The system tells the other contexts the values now in force, for example at start-up. */
public interface HomaValuesAnnouncer {

    AnnounceHomaValuesResponse announce();
}
