package reservation.usecase.port.in;

import reservation.usecase.dto.NameJobTeamRequest;
import reservation.usecase.dto.NameJobTeamResponse;

/** A provider names who is coming, before the booked start. */
public interface ProviderJobTeamNamer {

    NameJobTeamResponse name(NameJobTeamRequest request);
}
