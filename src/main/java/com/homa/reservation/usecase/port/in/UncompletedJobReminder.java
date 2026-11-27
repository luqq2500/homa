package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.RemindUncompletedJobsRequest;
import com.homa.reservation.usecase.dto.RemindUncompletedJobsResponse;

/** JOB-5: reminds providers of ended jobs they haven't marked Completed. */
public interface UncompletedJobReminder {

    RemindUncompletedJobsResponse remind(RemindUncompletedJobsRequest request);
}
