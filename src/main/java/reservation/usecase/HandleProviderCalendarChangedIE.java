package reservation.usecase;

import ie.events.ProviderCalendarChangedIE;
import reservation.domain.ProviderOpenTime;
import reservation.usecase.port.in.ProviderCalendarChangedIEHandler;
import reservation.usecase.port.out.ProviderOpenTimeRepository;

public final class HandleProviderCalendarChangedIE implements ProviderCalendarChangedIEHandler {

    private final ProviderOpenTimeRepository openTimes;

    public HandleProviderCalendarChangedIE(ProviderOpenTimeRepository openTimes) {
        this.openTimes = openTimes;
    }

    @Override
    public void handle(ProviderCalendarChangedIE event) {
        openTimes.save(new ProviderOpenTime(event.providerId(), event.openTime(), event.buffer()));
    }
}
