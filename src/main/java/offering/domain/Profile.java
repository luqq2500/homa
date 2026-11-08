package offering.domain;

import offering.domain.exception.ProfileNameRequiredException;
import java.util.Optional;

/** What customers see first: the provider's display name and profile picture. A picture can be added later. */
public record Profile(String displayName, Optional<String> picture) {

    public Profile {
        if (displayName == null || displayName.isBlank()) {
            throw new ProfileNameRequiredException();
        }
        displayName = displayName.strip();
        picture = picture.filter(reference -> !reference.isBlank()).map(String::strip);
    }

    public static Profile of(String displayName, String picture) {
        return new Profile(displayName, Optional.ofNullable(picture));
    }

    public static Profile withoutPicture(String displayName) {
        return new Profile(displayName, Optional.empty());
    }

    public boolean hasPicture() {
        return picture.isPresent();
    }
}
