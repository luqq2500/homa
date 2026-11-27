package com.homa.provider.usecase.port.in;

import com.homa.ie.events.ProfilePhotoChangedIE;

/** Takes Verification's latest word on a person's face photo (IDN-4), for the member they are or will be. */
public interface ProfilePhotoChangedIEHandler {

    void handle(ProfilePhotoChangedIE event);
}
