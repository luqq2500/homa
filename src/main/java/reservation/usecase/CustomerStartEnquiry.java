package reservation.usecase;

import ie.events.EnquiryStartedIE;
import java.time.Clock;
import reservation.domain.BookingDetails;
import reservation.domain.Enquiry;
import reservation.usecase.dto.StartEnquiryRequest;
import reservation.usecase.dto.StartEnquiryResponse;
import reservation.usecase.exception.ProviderNotFoundException;
import reservation.usecase.port.in.CustomerEnquiryStarter;
import reservation.usecase.port.out.EnquiryRepository;
import reservation.usecase.port.out.ProviderTermsRepository;
import reservation.usecase.port.out.ReservationIEPublisher;

public final class CustomerStartEnquiry implements CustomerEnquiryStarter {

    private final EnquiryRepository enquiries;
    private final ProviderTermsRepository providerTerms;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public CustomerStartEnquiry(EnquiryRepository enquiries, ProviderTermsRepository providerTerms,
                                ReservationIEPublisher publisher, Clock clock) {
        this.enquiries = enquiries;
        this.providerTerms = providerTerms;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public StartEnquiryResponse start(StartEnquiryRequest request) {
        if (providerTerms.find(request.providerId()).isEmpty()) {
            throw new ProviderNotFoundException(request.providerId());
        }
        Enquiry enquiry = Enquiry.start(request.customerId(), request.providerId(),
                new BookingDetails(request.jobIds(), request.start(), request.hours(), request.helpers()));
        enquiries.save(enquiry);
        publisher.publish(new EnquiryStartedIE(enquiry.id(), enquiry.customerId(), enquiry.providerId(),
                clock.instant()));
        return new StartEnquiryResponse(enquiry.id());
    }
}
