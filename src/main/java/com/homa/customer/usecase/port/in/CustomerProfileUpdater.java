package com.homa.customer.usecase.port.in;

import com.homa.customer.usecase.dto.UpdateProfileRequest;
import com.homa.customer.usecase.dto.UpdateProfileResponse;

/** A signed-in customer changes their own name, home address or arrival note; none can be left out (CUS-1). */
public interface CustomerProfileUpdater {

    UpdateProfileResponse update(UpdateProfileRequest request);
}
