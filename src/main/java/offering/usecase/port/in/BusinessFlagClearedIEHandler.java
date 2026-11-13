package offering.usecase.port.in;

import ie.events.BusinessFlagClearedIE;

/** A business whose flag is cleared resumes if it is verified again. */
public interface BusinessFlagClearedIEHandler {

    void handle(BusinessFlagClearedIE event);
}
