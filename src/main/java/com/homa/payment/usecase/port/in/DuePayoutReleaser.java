package com.homa.payment.usecase.port.in;

import com.homa.payment.usecase.dto.ReleaseDuePayoutsRequest;
import com.homa.payment.usecase.dto.ReleaseDuePayoutsResponse;

/** The scheduler releases every due payout to a verified payout account (PAY-2, PAY-3, PAY-8, PRV-2). */
public interface DuePayoutReleaser {

    ReleaseDuePayoutsResponse releaseDue(ReleaseDuePayoutsRequest request);
}
