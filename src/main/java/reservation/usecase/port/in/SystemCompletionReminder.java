package reservation.usecase.port.in;

import reservation.usecase.dto.RemindCompletionResponse;

/** Providers are reminded to submit completion after the completion reminder time. */
public interface SystemCompletionReminder {

    RemindCompletionResponse remindDue();
}
