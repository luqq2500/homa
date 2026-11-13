package catalogue.usecase.port.in;

import catalogue.usecase.dto.ViewShowcaseResponse;

/** Anyone sees the showcase: the featured searches that still have a job, in the order Homa sets. */
public interface GuestShowcaseViewer {

    ViewShowcaseResponse view();
}
