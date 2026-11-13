package reservation.usecase.port.in;

import reservation.usecase.dto.FileProviderReportRequest;
import reservation.usecase.dto.FileProviderReportResponse;

/** A provider files their one report on a reservation. */
public interface ProviderReportFiler {

    FileProviderReportResponse file(FileProviderReportRequest request);
}
