package offering.domain.exception;

public class ToolNotPickableException extends RuntimeException {

    public ToolNotPickableException(String toolId) {
        super("The tool " + toolId + " is not on Homa's tool list");
    }
}
