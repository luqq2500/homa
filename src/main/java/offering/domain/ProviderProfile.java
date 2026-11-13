package offering.domain;

import offering.domain.exception.ProfileIncompleteException;

/** What customers see first of a provider: the profile name and picture. */
public record ProviderProfile(String name, String picture) {

    public ProviderProfile {
        if (name == null || name.isBlank() || picture == null || picture.isBlank()) {
            throw new ProfileIncompleteException();
        }
        name = name.strip();
    }
}
