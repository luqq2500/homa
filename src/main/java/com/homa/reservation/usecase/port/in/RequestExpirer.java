package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.ExpireRequestRequest;
import com.homa.reservation.usecase.dto.ExpireRequestResponse;

/**
 * REQ-4: one Requested reservation past its acceptance deadline expires. This is the unit of work of the scheduler's
 * sweep ({@link OverdueRequestExpirer}): each call is its own transaction, so one reservation's conflict never
 * undoes another's expiry.
 */
public interface RequestExpirer {

    ExpireRequestResponse expire(ExpireRequestRequest request);
}
