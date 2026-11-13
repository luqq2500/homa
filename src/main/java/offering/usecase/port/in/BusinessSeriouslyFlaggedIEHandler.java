package offering.usecase.port.in;

import ie.events.BusinessSeriouslyFlaggedIE;

/** A seriously flagged business lapses at once, so its listing is paused. */
public interface BusinessSeriouslyFlaggedIEHandler {

    void handle(BusinessSeriouslyFlaggedIE event);
}
