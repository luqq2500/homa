package reservation.usecase;

import ie.events.HomaValuesChangedIE;
import reservation.domain.HomaValues;
import reservation.usecase.port.in.HomaValuesChangedIEHandler;
import reservation.usecase.port.out.HomaValuesRepository;

public final class HandleHomaValuesChangedIE implements HomaValuesChangedIEHandler {

    private final HomaValuesRepository homaValues;

    public HandleHomaValuesChangedIE(HomaValuesRepository homaValues) {
        this.homaValues = homaValues;
    }

    @Override
    public void handle(HomaValuesChangedIE event) {
        homaValues.save(new HomaValues(event.version(), event.serviceFee(), event.minimumHours(),
                event.maximumHours(), event.minimumNotice(), event.bookingHorizon(), event.teamReminderTime(),
                event.earliestCheckIn(), event.checkInPeriod(), event.checkInRadiusMetres(),
                event.customerNoShowWait(), event.customerNoShowFee(), event.noShowReportWindow(),
                event.requestExpiryTime(), event.responseDeadline(), event.completionReminderTime(),
                event.completionDeadline(), event.disputeWindowExtension(), event.feeWindow(),
                event.standardCancellationFee(), event.firmCancellationFee(), event.providerLateCancellationFine(),
                event.providerNoShowFine()));
    }
}
