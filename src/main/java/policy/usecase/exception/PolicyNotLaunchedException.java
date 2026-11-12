package policy.usecase.exception;

/** No Homa-set values are in force yet. */
public class PolicyNotLaunchedException extends PolicyUsecaseException {

    public PolicyNotLaunchedException() {
        super("The policy has not been launched yet");
    }
}
