package com.homa.verification.usecase.port.in;

import com.homa.verification.usecase.dto.RecordVerificationResultRequest;
import com.homa.verification.usecase.dto.RecordVerificationResultResponse;

/**
 * The verification service's webhook says a check has an answer: Homa verifies the person, or records why it
 * refused them (IDN-1 to IDN-3, CUS-10, PRV-1, HLP-1 to HLP-3).
 */
public interface VerificationResultRecorder {

    RecordVerificationResultResponse record(RecordVerificationResultRequest request);
}
