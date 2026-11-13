package reservation.usecase.port.in;

import reservation.usecase.dto.ReportCustomerNoShowRequest;
import reservation.usecase.dto.ReportCustomerNoShowResponse;

/** A provider who checked in reports that the customer is not there. */
public interface ProviderCustomerNoShowReporter {

    ReportCustomerNoShowResponse report(ReportCustomerNoShowRequest request);
}
