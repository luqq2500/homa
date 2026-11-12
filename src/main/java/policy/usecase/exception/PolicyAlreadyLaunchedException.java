package policy.usecase.exception;

/** The launch values are already in force; Homa changes them one at a time. */
public class PolicyAlreadyLaunchedException extends PolicyUsecaseException {

    public PolicyAlreadyLaunchedException(int version) {
        super("The policy is already launched; version " + version + " is in force");
    }
}
