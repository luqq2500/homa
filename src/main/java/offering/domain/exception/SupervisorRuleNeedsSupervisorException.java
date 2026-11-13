package offering.domain.exception;

public class SupervisorRuleNeedsSupervisorException extends RuntimeException {

    public SupervisorRuleNeedsSupervisorException() {
        super("A supervisor rule needs a supervisor on the team");
    }
}
