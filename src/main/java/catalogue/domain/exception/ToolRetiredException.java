package catalogue.domain.exception;

public class ToolRetiredException extends RuntimeException {

    public ToolRetiredException(String toolId) {
        super("The tool " + toolId + " is retired");
    }
}
