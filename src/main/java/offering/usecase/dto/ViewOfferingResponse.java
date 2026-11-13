package offering.usecase.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import kernel.City;
import kernel.Money;

public record ViewOfferingResponse(String listing, String stoppedBecause, List<String> missingToGoLive, String profileName, String profilePicture, Set<String> jobIds, Set<String> toolIds, int minHelpers, int maxHelpers, int supervisorFromHelpers, int supervisorFromHours, Money hourlyRate, boolean travelFeeSeparate, Money travelFee, int minHours, int maxHours, Set<City> cities, String cancellationTier, String acceptancePolicy, String insurer, Instant insuredUntil, int helpersOnTeam, int supervisorsOnTeam) {

    public ViewOfferingResponse {
        missingToGoLive = List.copyOf(missingToGoLive);
        jobIds = Set.copyOf(jobIds);
        toolIds = Set.copyOf(toolIds);
        cities = Set.copyOf(cities);
    }
}
