package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.ProfilePhotoChangedIE;

/** IDN-4, ROS-6: a person's photo was checked. */
public interface ProfilePhotoChangedIEHandler {

    void handle(ProfilePhotoChangedIE event);
}
