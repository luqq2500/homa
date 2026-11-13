package reservation.domain;

/** The three types of report. */
public enum ReportType {
    /** A problem during the job, for example a missing helper or missing tools. */
    DURING_THE_JOB,
    /** A problem with the finished work. */
    FINISHED_WORK,
    /** Safety, damage or loss: goes to the top of Homa's queue. */
    SAFETY_DAMAGE_OR_LOSS
}
