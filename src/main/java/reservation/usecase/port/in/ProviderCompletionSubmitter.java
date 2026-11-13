package reservation.usecase.port.in;

import reservation.usecase.dto.SubmitCompletionRequest;
import reservation.usecase.dto.SubmitCompletionResponse;

/** A provider marks the job completed with photo or video evidence. */
public interface ProviderCompletionSubmitter {

    SubmitCompletionResponse submit(SubmitCompletionRequest request);
}
