package com.homa.verification.usecase.port.in;

import com.homa.verification.usecase.dto.StartVerificationRequest;
import com.homa.verification.usecase.dto.StartVerificationResponse;

/**
 * A customer, an owner or a member starts identity verification (IDN-1, IDN-2, CUS-3, CUS-10, PRV-1, HLP-1 to
 * HLP-3). The verification service answers later, through {@link VerificationResultRecorder}.
 */
public interface VerificationStarter {

    StartVerificationResponse start(StartVerificationRequest request);
}
