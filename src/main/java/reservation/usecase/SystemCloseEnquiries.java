package reservation.usecase;

import ie.events.EnquiryClosedIE;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Enquiry;
import reservation.usecase.dto.CloseEnquiriesResponse;
import reservation.usecase.port.in.SystemEnquiryCloser;
import reservation.usecase.port.out.EnquiryRepository;
import reservation.usecase.port.out.ReservationIEPublisher;

public final class SystemCloseEnquiries implements SystemEnquiryCloser {

    private final EnquiryRepository enquiries;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public SystemCloseEnquiries(EnquiryRepository enquiries, ReservationIEPublisher publisher, Clock clock) {
        this.enquiries = enquiries;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public CloseEnquiriesResponse closePassed() {
        Instant now = clock.instant();
        List<String> ids = new ArrayList<>();
        for (Enquiry enquiry : enquiries.all()) {
            if (enquiry.closeIfDatePassed(now)) {
                enquiries.save(enquiry);
                publisher.publish(new EnquiryClosedIE(enquiry.id(), now));
                ids.add(enquiry.id());
            }
        }
        return new CloseEnquiriesResponse(ids);
    }
}
