package offering.usecase.port.in;

import ie.events.JobAddedIE;

/** A job Homa added can now be offered. */
public interface JobAddedIEHandler {

    void handle(JobAddedIE event);
}
