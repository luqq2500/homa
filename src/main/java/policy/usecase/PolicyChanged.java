package policy.usecase;

import ie.events.HomaPolicyChangedIE;
import policy.domain.CancellationTier;
import policy.domain.PolicySnapshot;

/** The event that carries a whole version of the Homa-set values, so listeners keep a full copy. */
final class PolicyChanged {

    private PolicyChanged() {
    }

    static HomaPolicyChangedIE from(PolicySnapshot s) {
        return new HomaPolicyChangedIE(
                s.version(), s.effectiveFrom(), s.serviceFee(), s.durationRange().minHours(), s.durationRange().maxHours(),
                s.minimumNotice(), s.bookingHorizon(), s.requestExpiry(), s.responseDeadline(), s.feeWindow(),
                s.cancellationFee(CancellationTier.FLEXIBLE), s.cancellationFee(CancellationTier.STANDARD),
                s.cancellationFee(CancellationTier.FIRM),
                s.providerLateCancellationFine(), s.providerNoShowFine(), s.fineDeductionCap(),
                s.teamReminder(), s.earliestCheckIn(), s.checkInPeriod(), s.checkInRadiusMetres(),
                s.customerNoShowWait(), s.customerNoShowFee(), s.noShowReportWindow(),
                s.completionReminder(), s.completionDeadline(), s.disputeWindowExtension(), s.evidenceRetention(),
                s.ratingWindow(), s.minimumRatingsToShow(), s.effectiveFrom());
    }
}
