package reservation.usecase.port.in;

import reservation.usecase.dto.ReleaseCustomerNoShowFeesResponse;

/** Customer no-show fees are released when the no-show report window ends without a challenge. */
public interface SystemCustomerNoShowFeeReleaser {

    ReleaseCustomerNoShowFeesResponse releaseDue();
}
