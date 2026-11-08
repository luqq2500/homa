package reservation.domain;

/** What a report is about. */
public enum ReportType {
    /** A problem during the job, such as a missing helper or missing tools. */
    DURING_JOB,
    /** A problem with the finished work. */
    FINISHED_WORK
}
