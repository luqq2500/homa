package catalogue.usecase.exception;

public class ToolNotFoundException extends RuntimeException {

    public ToolNotFoundException(String toolId) {
        super("No tool " + toolId);
    }
}
