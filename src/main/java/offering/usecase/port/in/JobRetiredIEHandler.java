package offering.usecase.port.in;

import ie.events.JobRetiredIE;

/** A retired job comes off every provider's listing. */
public interface JobRetiredIEHandler {

    void handle(JobRetiredIE event);
}
