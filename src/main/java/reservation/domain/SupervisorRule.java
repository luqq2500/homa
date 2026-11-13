package reservation.domain;

/** When the provider sends a supervisor: from a number of helpers, a number of hours, or both. 0 is not set. */
public record SupervisorRule(int fromHelpers, int fromHours) {

    /** A booking that reaches either threshold includes at least one supervisor, at no extra cost. */
    public boolean includesSupervisorFor(int helpers, int hours) {
        return reaches(fromHelpers, helpers) || reaches(fromHours, hours);
    }

    private static boolean reaches(int threshold, int booked) {
        return threshold > 0 && booked >= threshold;
    }
}
