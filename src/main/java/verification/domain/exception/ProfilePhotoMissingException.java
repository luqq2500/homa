package verification.domain.exception;

public class ProfilePhotoMissingException extends RuntimeException {

    public ProfilePhotoMissingException() {
        super("A team member is added with a profile photo taken with the camera");
    }
}
