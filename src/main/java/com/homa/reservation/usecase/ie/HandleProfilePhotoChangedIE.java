package com.homa.reservation.usecase.ie;

import com.homa.ie.events.ProfilePhotoChangedIE;
import com.homa.reservation.domain.replica.PersonPhoto;
import com.homa.reservation.usecase.port.in.ProfilePhotoChangedIEHandler;
import com.homa.reservation.usecase.port.out.PersonPhotoRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;
import com.homa.reservation.usecase.port.out.RosterableMemberRepository;

/**
 * IDN-4, ROS-6: Verification checked a person's photo. A verified photo becomes the one every member that person is
 * shows on new rosters; an unverified one changes nothing, and the old verified photo stays. Rosters already set
 * keep the photo they were set with.
 */
public final class HandleProfilePhotoChangedIE implements ProfilePhotoChangedIEHandler {

    static final String HANDLER = "reservation.ProfilePhotoChanged";

    private final ProcessedMessageStore processed;
    private final PersonPhotoRepository photos;
    private final RosterableMemberRepository members;

    public HandleProfilePhotoChangedIE(ProcessedMessageStore processed, PersonPhotoRepository photos,
            RosterableMemberRepository members) {
        this.processed = processed;
        this.photos = photos;
        this.members = members;
    }

    @Override
    public void handle(ProfilePhotoChangedIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        PersonPhoto photo = photos.findByPersonId(event.personId())
                .orElseGet(() -> PersonPhoto.none(event.personId()))
                .checked(event.verified(), event.photoRef());
        photos.save(photo);
        members.findByPersonId(event.personId())
                .forEach(member -> members.save(event.personId(), member.showing(photo)));
    }
}
