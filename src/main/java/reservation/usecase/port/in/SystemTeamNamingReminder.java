package reservation.usecase.port.in;

import reservation.usecase.dto.RemindTeamNamingResponse;

/** Providers are reminded at the team reminder time while the team is not named. */
public interface SystemTeamNamingReminder {

    RemindTeamNamingResponse remindDue();
}
