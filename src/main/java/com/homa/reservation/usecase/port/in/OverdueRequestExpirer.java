package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.ExpireOverdueRequestsRequest;
import com.homa.reservation.usecase.dto.ExpireOverdueRequestsResponse;

/** REQ-4: the scheduler expires every Requested reservation past its acceptance deadline. */
public interface OverdueRequestExpirer {

    ExpireOverdueRequestsResponse expireOverdue(ExpireOverdueRequestsRequest request);
}
