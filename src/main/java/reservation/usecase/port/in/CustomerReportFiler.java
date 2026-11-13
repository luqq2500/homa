package reservation.usecase.port.in;

import reservation.usecase.dto.FileCustomerReportRequest;
import reservation.usecase.dto.FileCustomerReportResponse;

/** A customer files their one report on a reservation. */
public interface CustomerReportFiler {

    FileCustomerReportResponse file(FileCustomerReportRequest request);
}
