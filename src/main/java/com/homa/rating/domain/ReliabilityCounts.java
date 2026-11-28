package com.homa.rating.domain;

/**
 * What reliability shows about a party: counts over the last 12 months (RTG-10). A customer has no no-rosters and no
 * members who left, so those counts are 0 for them.
 */
public record ReliabilityCounts(int cancellations, int noRosters, int noShows, int membersLeft) {
}
